package app.bevoya.api.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bevoya.api.testsupport.TestTokens;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** Skills against a real database, as bevoya_app, with real tokens. Needs Docker (CI). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class SkillApiTest {

  private static final String APP_PASSWORD = "app-test-only";
  private static final Pattern ID = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"");

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
      .withDatabaseName("bevoya")
      .withUsername("postgres")
      .withPassword("postgres")
      .withCopyFileToContainer(
          MountableFile.forHostPath("../infrastructure/db/ci-bootstrap.sql"),
          "/docker-entrypoint-initdb.d/00-ci-bootstrap.sql");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", () -> "bevoya_app");
    registry.add("spring.datasource.password", () -> APP_PASSWORD);
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", TestTokens::jwksUri);
  }

  @Value("${local.server.port}")
  int port;

  private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  @BeforeAll
  static void migrate() throws Exception {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
    try (Connection c = admin(); Statement s = c.createStatement()) {
      s.execute("alter role bevoya_app password '" + APP_PASSWORD + "'");
    }
  }

  @AfterAll
  static void stopJwks() {
    TestTokens.stop();
  }

  // ---- helpers ----

  private static Connection admin() throws SQLException {
    return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static UUID signedUpUser() throws SQLException {
    UUID id = UUID.randomUUID();
    try (Connection c = admin(); var s = c.prepareStatement("insert into auth.users (id) values (?)")) {
      s.setObject(1, id);
      s.executeUpdate();
    }
    return id;
  }

  private static long skillCount(UUID user) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("select count(*) from public.skills where user_id = ?")) {
      s.setObject(1, user);
      try (ResultSet rs = s.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private static void insertEvidence(UUID user, UUID skill, String level) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("insert into public.evidence (user_id, skill_id, type, activity_level, "
            + "title) values (?, ?, 'note', ?, 'Linked')")) {
      s.setObject(1, user);
      s.setObject(2, skill);
      s.setString(3, level);
      s.executeUpdate();
    }
  }

  private static void insertNote(UUID user, UUID skill) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("insert into public.learning_entries (user_id, skill_id, title, content) "
            + "values (?, ?, 'Linked', 'Linked')")) {
      s.setObject(1, user);
      s.setObject(2, skill);
      s.executeUpdate();
    }
  }

  private static String noteSkillId(UUID user) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("select skill_id from public.learning_entries where user_id = ?")) {
      s.setObject(1, user);
      try (ResultSet rs = s.executeQuery()) {
        assertTrue(rs.next(), "the note must still exist");
        return rs.getString(1);
      }
    }
  }

  private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
    return callWithKey(method, path, token, body, null);
  }

  private HttpResponse<String> callWithKey(String method, String path, String token, String body,
      String key) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    if (key != null) {
      request.header("Idempotency-Key", key);
    }
    if (body != null) {
      request.header("Content-Type", "application/json");
      request.method(method, HttpRequest.BodyPublishers.ofString(body));
    } else {
      request.method(method, HttpRequest.BodyPublishers.noBody());
    }
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static String tokenFor(UUID user) {
    return TestTokens.tokenFor(user);
  }

  private static UUID idOf(HttpResponse<String> response) {
    Matcher m = ID.matcher(response.body());
    assertTrue(m.find(), response.body());
    return UUID.fromString(m.group(1));
  }

  private static String field(String body, String name) {
    Matcher m = Pattern.compile("\"" + name + "\":(null|true|false|\"[^\"]*\"|[0-9.\\-]+)").matcher(body);
    assertTrue(m.find(), body);
    return m.group(1);
  }

  private static String withoutPerRequestFields(String body) {
    return body.replaceAll("\"(requestId|instance)\":\"[^\"]*\",?", "");
  }

  private static void assertProblem(HttpResponse<String> response, int status) {
    assertEquals(status, response.statusCode(), response.body());
    assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/problem+json"),
        response.headers().firstValue("content-type").orElse("none"));
  }

  private static String skill(String name) {
    return "{\"name\":\"" + name + "\"}";
  }

  private HttpResponse<String> post(UUID user, String json) throws Exception {
    return call("POST", "/skills", tokenFor(user), json);
  }

  private UUID createOk(UUID user, String json) throws Exception {
    HttpResponse<String> created = post(user, json);
    assertEquals(201, created.statusCode(), created.body());
    return idOf(created);
  }

  private String levelOf(UUID user, UUID skill) throws Exception {
    HttpResponse<String> read = call("GET", "/skills/" + skill, tokenFor(user), null);
    assertEquals(200, read.statusCode(), read.body());
    return field(read.body(), "level");
  }

  // ---- tests ----

  @Test
  void createReturnsTheSkillAtDiscoveredAndItIsReadableAgain() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = post(alice, "{\"name\":\"  Python  \",\"category\":\"coding\","
        + "\"description\":\"Backend work\",\"targetLevel\":\"built\","
        + "\"nextRevisionAt\":\"2026-11-20T09:00:00Z\"}");
    assertEquals(201, created.statusCode(), created.body());
    UUID id = idOf(created);
    assertEquals("/skills/" + id, created.headers().firstValue("location").orElse(""));
    assertEquals("\"Python\"", field(created.body(), "name"));
    assertEquals("\"discovered\"", field(created.body(), "level"));
    assertEquals("\"built\"", field(created.body(), "targetLevel"));
    assertEquals("0", field(created.body(), "evidenceCount"));
    assertEquals("0", field(created.body(), "learningEntryCount"));
    assertEquals("null", field(created.body(), "lastPracticedAt"));

    HttpResponse<String> bare = post(alice, skill("SQL"));
    assertEquals(201, bare.statusCode(), bare.body());
    assertEquals("null", field(bare.body(), "category"));
    assertEquals("null", field(bare.body(), "targetLevel"));
    assertEquals("null", field(bare.body(), "nextRevisionAt"));

    HttpResponse<String> read = call("GET", "/skills/" + id, tokenFor(alice), null);
    assertEquals(200, read.statusCode(), read.body());
    assertEquals(created.body(), read.body());
  }

  @Test
  void aSkillThatIsMissingAndASkillThatIsNotYoursGiveTheSame404() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID item = createOk(alice, skill("Alice only"));
    UUID missing = UUID.randomUUID();
    String put = "{\"name\":\"Hijacked\"}";

    String[][] calls = {{"GET", null}, {"PUT", put}, {"DELETE", null}};
    for (String[] c : calls) {
      HttpResponse<String> foreign = call(c[0], "/skills/" + item, tokenFor(bob), c[1]);
      HttpResponse<String> absent = call(c[0], "/skills/" + missing, tokenFor(bob), c[1]);
      assertProblem(foreign, 404);
      assertProblem(absent, 404);
      assertEquals(withoutPerRequestFields(absent.body()), withoutPerRequestFields(foreign.body()), c[0]);
    }
    HttpResponse<String> mine = call("GET", "/skills/" + item, tokenFor(alice), null);
    assertEquals(200, mine.statusCode(), mine.body());
    assertEquals("\"Alice only\"", field(mine.body(), "name"));
  }

  @Test
  void namesAreUniquePerPersonIgnoringCase() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID python = createOk(alice, skill("Python"));
    assertProblem(post(alice, skill("python")), 409);
    assertProblem(post(alice, skill("  PYTHON  ")), 409);
    assertEquals(201, post(bob, skill("Python")).statusCode(), "another person may use the same name");

    UUID sql = createOk(alice, skill("SQL"));
    assertProblem(call("PUT", "/skills/" + sql, tokenFor(alice), skill("python")), 409);
    assertEquals(200, call("PUT", "/skills/" + python, tokenFor(alice), skill("PYTHON")).statusCode(),
        "renaming to its own name in another case is allowed");
    assertEquals(2, skillCount(alice));
  }

  @Test
  void listsOnlyYourOwnSkillsByName() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID banana = createOk(alice, skill("banana"));
    UUID apple = createOk(alice, skill("Apple"));
    createOk(bob, skill("Bob's"));

    HttpResponse<String> list = call("GET", "/skills", tokenFor(alice), null);
    assertEquals(200, list.statusCode(), list.body());
    List<UUID> ids = new ArrayList<>();
    Matcher m = ID.matcher(list.body());
    while (m.find()) {
      ids.add(UUID.fromString(m.group(1)));
    }
    assertEquals(List.of(apple, banana), ids);
  }

  @Test
  void putReplacesEverythingAndMovesUpdatedAt() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = post(alice, "{\"name\":\"Old\",\"category\":\"x\",\"description\":\"d\","
        + "\"targetLevel\":\"tested\",\"nextRevisionAt\":\"2026-12-01T00:00:00Z\"}");
    UUID id = idOf(created);

    HttpResponse<String> replaced = call("PUT", "/skills/" + id, tokenFor(alice), skill("  New  "));
    assertEquals(200, replaced.statusCode(), replaced.body());
    assertEquals("\"New\"", field(replaced.body(), "name"));
    assertEquals("null", field(replaced.body(), "category"), "an omitted optional field is cleared");
    assertEquals("null", field(replaced.body(), "description"));
    assertEquals("null", field(replaced.body(), "targetLevel"));
    assertEquals("null", field(replaced.body(), "nextRevisionAt"));
    assertEquals(field(created.body(), "createdAt"), field(replaced.body(), "createdAt"));
    assertNotEquals(field(created.body(), "updatedAt"), field(replaced.body(), "updatedAt"));

    HttpResponse<String> reread = call("GET", "/skills/" + id, tokenFor(alice), null);
    assertEquals(replaced.body(), reread.body(), "the update must be persisted, not only returned");
  }

  @Test
  void deleteRemovesASkillOnceAndKeepsLinkedNotes() throws Exception {
    UUID alice = signedUpUser();
    UUID id = createOk(alice, skill("Temporary"));
    insertNote(alice, id);
    assertEquals(204, call("DELETE", "/skills/" + id, tokenFor(alice), null).statusCode());
    assertProblem(call("DELETE", "/skills/" + id, tokenFor(alice), null), 404);
    assertProblem(call("GET", "/skills/" + id, tokenFor(alice), null), 404);
    assertEquals(0, skillCount(alice));
    assertEquals(null, noteSkillId(alice), "the note is kept and unlinked");
  }

  @Test
  void levelIsDerivedFromLinkedNotesAndEvidence() throws Exception {
    UUID alice = signedUpUser();
    UUID id = createOk(alice, "{\"name\":\"RAG\",\"targetLevel\":\"applied\"}");
    assertEquals("\"discovered\"", levelOf(alice, id));

    insertEvidence(alice, id, "watched");
    assertEquals("\"studying\"", levelOf(alice, id));
    insertNote(alice, id);
    assertEquals("\"explained\"", levelOf(alice, id));
    insertEvidence(alice, id, "practiced");
    assertEquals("\"practiced\"", levelOf(alice, id));
    insertEvidence(alice, id, "built");
    assertEquals("\"built\"", levelOf(alice, id));
    insertEvidence(alice, id, "tested");
    assertEquals("\"tested\"", levelOf(alice, id));
    insertEvidence(alice, id, "applied");

    HttpResponse<String> read = call("GET", "/skills/" + id, tokenFor(alice), null);
    assertEquals("\"applied\"", field(read.body(), "level"));
    assertEquals("\"applied\"", field(read.body(), "targetLevel"));
    assertEquals("6", field(read.body(), "evidenceCount"));
    assertEquals("1", field(read.body(), "learningEntryCount"));
    assertTrue(!field(read.body(), "lastPracticedAt").equals("null"));
  }

  @Test
  void invalidInputIsRejectedWithAProblemAndChangesNothing() throws Exception {
    UUID alice = signedUpUser();
    List<String> badBodies = List.of(
        "{}",
        "{\"name\":\"\"}",
        "{\"name\":\"   \"}",
        "{\"name\":\"" + "n".repeat(101) + "\"}",
        "{\"name\":\"x\",\"category\":\"" + "c".repeat(51) + "\"}",
        "{\"name\":\"x\",\"description\":\"" + "d".repeat(1001) + "\"}",
        "{\"name\":\"x\",\"targetLevel\":\"expert\"}",
        "{\"name\":\"x\",\"nextRevisionAt\":\"tomorrow\"}",
        "{not json");
    for (String body : badBodies) {
      assertProblem(post(alice, body), 400);
    }
    UUID id = createOk(alice, skill("Valid"));
    for (String body : badBodies) {
      assertProblem(call("PUT", "/skills/" + id, tokenFor(alice), body), 400);
    }
    assertProblem(call("GET", "/skills/not-a-uuid", tokenFor(alice), null), 400);
    assertEquals("\"Valid\"", field(call("GET", "/skills/" + id, tokenFor(alice), null).body(), "name"));
    assertEquals(1, skillCount(alice));
  }

  @Test
  void missingTokenIsRejectedOnEveryRoute() throws Exception {
    String id = UUID.randomUUID().toString();
    String[][] routes = {
        {"POST", "/skills", skill("x")},
        {"GET", "/skills", null},
        {"GET", "/skills/" + id, null},
        {"PUT", "/skills/" + id, skill("x")},
        {"DELETE", "/skills/" + id, null}};
    for (String[] route : routes) {
      HttpResponse<String> response = call(route[0], route[1], null, route[2]);
      assertProblem(response, 401);
      assertTrue(response.headers().firstValue("www-authenticate").orElse("").startsWith("Bearer"));
    }
  }

  @Test
  void sameKeyTwiceCreatesOneSkillAndReturnsTheOriginal() throws Exception {
    UUID alice = signedUpUser();
    String key = UUID.randomUUID().toString();
    HttpResponse<String> first = callWithKey("POST", "/skills", tokenFor(alice), skill("Original"), key);
    HttpResponse<String> retry = callWithKey("POST", "/skills", tokenFor(alice), skill("Different"), key);
    assertEquals(201, first.statusCode(), first.body());
    assertEquals(201, retry.statusCode(), retry.body());
    assertEquals(idOf(first), idOf(retry));
    assertEquals(first.body(), retry.body());
    assertEquals(1, skillCount(alice));

    for (String bad : List.of("short", "has spaces in the key", "x".repeat(101))) {
      assertProblem(callWithKey("POST", "/skills", tokenFor(alice), skill("Nope"), bad), 400);
    }
    assertEquals(1, skillCount(alice));
  }
}