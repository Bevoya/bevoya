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

/** Learning entries against a real database, as bevoya_app, with real tokens. Needs Docker (CI). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class LearningEntryApiTest {

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

  private static long entryCount(UUID user) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("select count(*) from public.learning_entries where user_id = ?")) {
      s.setObject(1, user);
      try (ResultSet rs = s.executeQuery()) {
        rs.next();
        return rs.getLong(1);
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

  private static String entry(String title) {
    return "{\"title\":\"" + title + "\",\"content\":\"Metadata filtering improves retrieval relevance\"}";
  }

  private HttpResponse<String> post(UUID user, String json) throws Exception {
    return call("POST", "/learning-entries", tokenFor(user), json);
  }

  private UUID createOk(UUID user, String json) throws Exception {
    HttpResponse<String> created = post(user, json);
    assertEquals(201, created.statusCode(), created.body());
    return idOf(created);
  }

  private List<UUID> idsOf(String path, UUID user) throws Exception {
    HttpResponse<String> response = call("GET", path, tokenFor(user), null);
    assertEquals(200, response.statusCode(), response.body());
    List<UUID> ids = new ArrayList<>();
    Matcher m = ID.matcher(response.body());
    while (m.find()) {
      ids.add(UUID.fromString(m.group(1)));
    }
    return ids;
  }

  // ---- tests ----

  @Test
  void createReturnsTheEntryWithOptionalFieldsAndItIsReadableAgain() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = post(alice, "{\"title\":\"  RAG basics  \",\"content\":\"Chunking matters\","
        + "\"sourceUrl\":\"https://docs.example.com/rag\",\"confidence\":3}");
    assertEquals(201, created.statusCode(), created.body());
    UUID id = idOf(created);
    assertEquals("/learning-entries/" + id, created.headers().firstValue("location").orElse(""));
    assertEquals("\"RAG basics\"", field(created.body(), "title"));
    assertEquals("\"https://docs.example.com/rag\"", field(created.body(), "sourceUrl"));
    assertEquals("3", field(created.body(), "confidence"));

    HttpResponse<String> bare = post(alice, entry("No extras"));
    assertEquals(201, bare.statusCode(), bare.body());
    assertEquals("null", field(bare.body(), "sourceUrl"));
    assertEquals("null", field(bare.body(), "confidence"));

    HttpResponse<String> read = call("GET", "/learning-entries/" + id, tokenFor(alice), null);
    assertEquals(200, read.statusCode(), read.body());
    assertEquals(created.body(), read.body());
  }

  @Test
  void anEntryThatIsMissingAndAnEntryThatIsNotYoursGiveTheSame404() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID item = createOk(alice, entry("Alice's note"));
    UUID missing = UUID.randomUUID();
    String put = "{\"title\":\"Hijacked\",\"content\":\"Hijacked\"}";

    String[][] calls = {{"GET", null}, {"PUT", put}, {"DELETE", null}};
    for (String[] c : calls) {
      HttpResponse<String> foreign = call(c[0], "/learning-entries/" + item, tokenFor(bob), c[1]);
      HttpResponse<String> absent = call(c[0], "/learning-entries/" + missing, tokenFor(bob), c[1]);
      assertProblem(foreign, 404);
      assertProblem(absent, 404);
      assertEquals(withoutPerRequestFields(absent.body()), withoutPerRequestFields(foreign.body()), c[0]);
    }
    HttpResponse<String> mine = call("GET", "/learning-entries/" + item, tokenFor(alice), null);
    assertEquals(200, mine.statusCode(), mine.body());
    assertEquals("\"Alice's note\"", field(mine.body(), "title"));
  }

  @Test
  void listsOnlyYourOwnEntriesNewestFirst() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID first = createOk(alice, entry("First"));
    UUID second = createOk(alice, entry("Second"));
    createOk(bob, entry("Bob's"));

    assertEquals(List.of(second, first), idsOf("/learning-entries", alice));
    assertEquals(1, idsOf("/learning-entries", bob).size());
  }

  @Test
  void putReplacesEverythingAndMovesUpdatedAt() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = post(alice, "{\"title\":\"Old\",\"content\":\"Old content\","
        + "\"sourceUrl\":\"https://docs.example.com/old\",\"confidence\":2}");
    UUID id = idOf(created);

    HttpResponse<String> replaced = call("PUT", "/learning-entries/" + id, tokenFor(alice),
        "{\"title\":\"  New  \",\"content\":\"New content\"}");
    assertEquals(200, replaced.statusCode(), replaced.body());
    assertEquals("\"New\"", field(replaced.body(), "title"));
    assertEquals("\"New content\"", field(replaced.body(), "content"));
    assertEquals("null", field(replaced.body(), "sourceUrl"), "a missing optional field is cleared");
    assertEquals("null", field(replaced.body(), "confidence"));
    assertEquals(field(created.body(), "createdAt"), field(replaced.body(), "createdAt"));
    assertNotEquals(field(created.body(), "updatedAt"), field(replaced.body(), "updatedAt"));

    HttpResponse<String> reread = call("GET", "/learning-entries/" + id, tokenFor(alice), null);
    assertEquals(replaced.body(), reread.body(), "the update must be persisted, not only returned");
  }

  @Test
  void deleteRemovesAnEntryOnce() throws Exception {
    UUID alice = signedUpUser();
    UUID id = createOk(alice, entry("Temporary"));
    assertEquals(204, call("DELETE", "/learning-entries/" + id, tokenFor(alice), null).statusCode());
    assertProblem(call("DELETE", "/learning-entries/" + id, tokenFor(alice), null), 404);
    assertProblem(call("GET", "/learning-entries/" + id, tokenFor(alice), null), 404);
    assertEquals(0, entryCount(alice));
  }

  @Test
  void invalidInputIsRejectedWithAProblemAndChangesNothing() throws Exception {
    UUID alice = signedUpUser();
    List<String> badBodies = List.of(
        "{}",
        "{\"content\":\"x\"}",
        "{\"title\":\"x\"}",
        "{\"title\":\"\",\"content\":\"x\"}",
        "{\"title\":\"   \",\"content\":\"x\"}",
        "{\"title\":\"x\",\"content\":\"   \"}",
        "{\"title\":\"" + "t".repeat(201) + "\",\"content\":\"x\"}",
        "{\"title\":\"x\",\"content\":\"" + "c".repeat(20001) + "\"}",
        "{\"title\":\"x\",\"content\":\"x\",\"confidence\":0}",
        "{\"title\":\"x\",\"content\":\"x\",\"confidence\":6}",
        "{\"title\":\"x\",\"content\":\"x\",\"sourceUrl\":\"http://example.com\"}",
        "{\"title\":\"x\",\"content\":\"x\",\"sourceUrl\":\"https://has space.com\"}",
        "{not json");
    for (String body : badBodies) {
      assertProblem(post(alice, body), 400);
    }
    UUID id = createOk(alice, entry("Valid"));
    for (String body : badBodies) {
      assertProblem(call("PUT", "/learning-entries/" + id, tokenFor(alice), body), 400);
    }
    assertProblem(call("GET", "/learning-entries/not-a-uuid", tokenFor(alice), null), 400);
    assertEquals("\"Valid\"", field(call("GET", "/learning-entries/" + id, tokenFor(alice), null).body(), "title"));
    assertEquals(1, entryCount(alice));
  }

  @Test
  void missingTokenIsRejectedOnEveryRoute() throws Exception {
    String id = UUID.randomUUID().toString();
    String[][] routes = {
        {"POST", "/learning-entries", entry("x")},
        {"GET", "/learning-entries", null},
        {"GET", "/learning-entries/" + id, null},
        {"PUT", "/learning-entries/" + id, entry("x")},
        {"DELETE", "/learning-entries/" + id, null}};
    for (String[] route : routes) {
      HttpResponse<String> response = call(route[0], route[1], null, route[2]);
      assertProblem(response, 401);
      assertTrue(response.headers().firstValue("www-authenticate").orElse("").startsWith("Bearer"));
    }
  }

  @Test
  void sameKeyTwiceCreatesOneEntryAndReturnsTheOriginal() throws Exception {
    UUID alice = signedUpUser();
    String key = UUID.randomUUID().toString();
    HttpResponse<String> first = callWithKey("POST", "/learning-entries", tokenFor(alice), entry("Original"), key);
    HttpResponse<String> retry = callWithKey("POST", "/learning-entries", tokenFor(alice), entry("Different"), key);
    assertEquals(201, first.statusCode(), first.body());
    assertEquals(201, retry.statusCode(), retry.body());
    assertEquals(idOf(first), idOf(retry));
    assertEquals(first.body(), retry.body());
    assertEquals(1, entryCount(alice));
  }

  @Test
  void keysAreScopedPerUserMalformedKeysAreRefusedAndADeletedEntryReleasesItsKey() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    String key = UUID.randomUUID().toString();
    UUID aliceItem = idOf(callWithKey("POST", "/learning-entries", tokenFor(alice), entry("Alice"), key));
    UUID bobItem = idOf(callWithKey("POST", "/learning-entries", tokenFor(bob), entry("Bob"), key));
    assertNotEquals(aliceItem, bobItem);

    for (String bad : List.of("short", "has spaces in the key", "x".repeat(101))) {
      assertProblem(callWithKey("POST", "/learning-entries", tokenFor(alice), entry("Nope"), bad), 400);
    }
    assertEquals(1, entryCount(alice));

    assertEquals(204, call("DELETE", "/learning-entries/" + aliceItem, tokenFor(alice), null).statusCode());
    HttpResponse<String> again = callWithKey("POST", "/learning-entries", tokenFor(alice), entry("Alice"), key);
    assertEquals(201, again.statusCode(), again.body());
    assertNotEquals(aliceItem, idOf(again));
    assertEquals(1, entryCount(alice));
  }
}