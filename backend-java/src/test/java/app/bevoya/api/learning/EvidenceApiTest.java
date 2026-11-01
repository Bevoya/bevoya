package app.bevoya.api.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

/** Evidence against a real database, as bevoya_app, with real tokens. Needs Docker (CI). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class EvidenceApiTest {

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

  private static long evidenceCount(UUID user) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("select count(*) from public.evidence where user_id = ?")) {
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

  private static String evidence(String title) {
    return "{\"type\":\"note\",\"activityLevel\":\"practiced\",\"title\":\"" + title + "\"}";
  }

  private HttpResponse<String> post(UUID user, String json) throws Exception {
    return call("POST", "/evidence", tokenFor(user), json);
  }

  private UUID createOk(UUID user, String json) throws Exception {
    HttpResponse<String> created = post(user, json);
    assertEquals(201, created.statusCode(), created.body());
    return idOf(created);
  }

  private UUID startSession(UUID user) throws Exception {
    HttpResponse<String> started = call("POST", "/sessions", tokenFor(user), "{\"activityType\":\"study\"}");
    assertEquals(201, started.statusCode(), started.body());
    return idOf(started);
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
  void createReturnsTheEvidenceAndItIsReadableAgain() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = post(alice, "{\"type\":\"commit\",\"activityLevel\":\"built\","
        + "\"title\":\"  Vector search endpoint  \",\"content\":\"Added metadata filtering\","
        + "\"externalUrl\":\"https://github.com/example/repo/commit/abc123\"}");
    assertEquals(201, created.statusCode(), created.body());
    UUID id = idOf(created);
    assertEquals("/evidence/" + id, created.headers().firstValue("location").orElse(""));
    assertEquals("\"Vector search endpoint\"", field(created.body(), "title"));
    assertEquals("\"commit\"", field(created.body(), "type"));
    assertEquals("\"built\"", field(created.body(), "activityLevel"));
    assertEquals("null", field(created.body(), "sessionId"));

    HttpResponse<String> read = call("GET", "/evidence/" + id, tokenFor(alice), null);
    assertEquals(200, read.statusCode(), read.body());
    assertEquals(created.body(), read.body());
  }

  @Test
  void evidenceThatIsMissingAndEvidenceThatIsNotYoursGiveTheSame404() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID item = createOk(alice, evidence("Alice's evidence"));
    UUID missing = UUID.randomUUID();

    for (String method : List.of("GET", "DELETE")) {
      HttpResponse<String> foreign = call(method, "/evidence/" + item, tokenFor(bob), null);
      HttpResponse<String> absent = call(method, "/evidence/" + missing, tokenFor(bob), null);
      assertProblem(foreign, 404);
      assertProblem(absent, 404);
      assertEquals(withoutPerRequestFields(absent.body()), withoutPerRequestFields(foreign.body()), method);
    }
    assertEquals(200, call("GET", "/evidence/" + item, tokenFor(alice), null).statusCode());
    assertEquals(1, evidenceCount(alice));
  }

  @Test
  void listsOnlyYourOwnEvidenceNewestFirstAndFiltersBySessionAndLevel() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID session = startSession(alice);
    UUID first = createOk(alice, "{\"type\":\"note\",\"activityLevel\":\"watched\",\"title\":\"First\"}");
    UUID second = createOk(alice, "{\"type\":\"note\",\"activityLevel\":\"built\",\"title\":\"Second\","
        + "\"sessionId\":\"" + session + "\"}");
    createOk(bob, evidence("Bob's"));

    assertEquals(List.of(second, first), idsOf("/evidence", alice));
    assertEquals(1, idsOf("/evidence", bob).size());
    assertEquals(List.of(second), idsOf("/evidence?sessionId=" + session, alice));
    assertEquals(List.of(first), idsOf("/evidence?activityLevel=watched", alice));
    assertEquals(List.of(), idsOf("/evidence?sessionId=" + UUID.randomUUID(), alice));
    assertProblem(call("GET", "/evidence?activityLevel=nope", tokenFor(alice), null), 400);
    assertProblem(call("GET", "/evidence?sessionId=not-a-uuid", tokenFor(alice), null), 400);
  }

  @Test
  void evidenceCanOnlyLinkToYourOwnSession() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID aliceSession = startSession(alice);

    assertProblem(post(bob, "{\"type\":\"note\",\"activityLevel\":\"read\",\"title\":\"x\","
        + "\"sessionId\":\"" + aliceSession + "\"}"), 404);
    assertProblem(post(bob, "{\"type\":\"note\",\"activityLevel\":\"read\",\"title\":\"x\","
        + "\"sessionId\":\"" + UUID.randomUUID() + "\"}"), 404);
    assertEquals(0, evidenceCount(bob));

    HttpResponse<String> linked = post(alice, "{\"type\":\"note\",\"activityLevel\":\"read\",\"title\":\"x\","
        + "\"sessionId\":\"" + aliceSession + "\"}");
    assertEquals(201, linked.statusCode(), linked.body());
    assertEquals("\"" + aliceSession + "\"", field(linked.body(), "sessionId"));
  }

  @Test
  void deletingASessionKeepsItsEvidenceButUnlinksIt() throws Exception {
    UUID alice = signedUpUser();
    UUID session = startSession(alice);
    UUID id = createOk(alice, "{\"type\":\"note\",\"activityLevel\":\"built\",\"title\":\"Kept\","
        + "\"sessionId\":\"" + session + "\"}");

    assertEquals(204, call("DELETE", "/sessions/" + session, tokenFor(alice), null).statusCode());
    HttpResponse<String> read = call("GET", "/evidence/" + id, tokenFor(alice), null);
    assertEquals(200, read.statusCode(), read.body());
    assertEquals("null", field(read.body(), "sessionId"));
  }

  @Test
  void deleteRemovesEvidenceOnce() throws Exception {
    UUID alice = signedUpUser();
    UUID id = createOk(alice, evidence("Temporary"));
    assertEquals(204, call("DELETE", "/evidence/" + id, tokenFor(alice), null).statusCode());
    assertProblem(call("DELETE", "/evidence/" + id, tokenFor(alice), null), 404);
    assertProblem(call("GET", "/evidence/" + id, tokenFor(alice), null), 404);
    assertEquals(0, evidenceCount(alice));
  }

  @Test
  void invalidInputIsRejectedWithAProblemAndCreatesNothing() throws Exception {
    UUID alice = signedUpUser();
    List<String> badBodies = List.of(
        "{}",
        "{\"activityLevel\":\"built\",\"title\":\"x\"}",
        "{\"type\":\"note\",\"title\":\"x\"}",
        "{\"type\":\"note\",\"activityLevel\":\"built\"}",
        "{\"type\":\"note\",\"activityLevel\":\"built\",\"title\":\"\"}",
        "{\"type\":\"note\",\"activityLevel\":\"built\",\"title\":\"   \"}",
        "{\"type\":\"nope\",\"activityLevel\":\"built\",\"title\":\"x\"}",
        "{\"type\":\"note\",\"activityLevel\":\"nope\",\"title\":\"x\"}",
        "{\"type\":\"note\",\"activityLevel\":\"built\",\"title\":\"" + "t".repeat(201) + "\"}",
        "{\"type\":\"note\",\"activityLevel\":\"built\",\"title\":\"x\",\"content\":\"" + "c".repeat(10001) + "\"}",
        "{\"type\":\"link\",\"activityLevel\":\"read\",\"title\":\"x\",\"externalUrl\":\"http://example.com\"}",
        "{\"type\":\"link\",\"activityLevel\":\"read\",\"title\":\"x\",\"externalUrl\":\"javascript:alert(1)\"}",
        "{\"type\":\"link\",\"activityLevel\":\"read\",\"title\":\"x\",\"externalUrl\":\"https://has space.com\"}",
        "{\"type\":\"note\",\"activityLevel\":\"built\",\"title\":\"x\",\"sessionId\":\"not-a-uuid\"}",
        "{not json");
    for (String body : badBodies) {
      assertProblem(post(alice, body), 400);
    }
    assertProblem(call("GET", "/evidence/not-a-uuid", tokenFor(alice), null), 400);
    assertEquals(0, evidenceCount(alice));
  }

  @Test
  void missingTokenIsRejectedOnEveryRoute() throws Exception {
    String id = UUID.randomUUID().toString();
    String[][] routes = {
        {"POST", "/evidence", evidence("x")},
        {"GET", "/evidence", null},
        {"GET", "/evidence/" + id, null},
        {"DELETE", "/evidence/" + id, null}};
    for (String[] route : routes) {
      HttpResponse<String> response = call(route[0], route[1], null, route[2]);
      assertProblem(response, 401);
      assertTrue(response.headers().firstValue("www-authenticate").orElse("").startsWith("Bearer"));
    }
  }

  @Test
  void sameKeyTwiceCreatesOneEvidenceAndReturnsTheOriginal() throws Exception {
    UUID alice = signedUpUser();
    String key = UUID.randomUUID().toString();
    HttpResponse<String> first = callWithKey("POST", "/evidence", tokenFor(alice), evidence("Original"), key);
    HttpResponse<String> retry = callWithKey("POST", "/evidence", tokenFor(alice), evidence("Different"), key);
    assertEquals(201, first.statusCode(), first.body());
    assertEquals(201, retry.statusCode(), retry.body());
    assertEquals(idOf(first), idOf(retry));
    assertEquals(first.body(), retry.body());
    assertEquals(1, evidenceCount(alice));
  }

  @Test
  void keysAreScopedPerUserMalformedKeysAreRefusedAndADeletedItemReleasesItsKey() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    String key = UUID.randomUUID().toString();
    UUID aliceItem = idOf(callWithKey("POST", "/evidence", tokenFor(alice), evidence("Alice"), key));
    UUID bobItem = idOf(callWithKey("POST", "/evidence", tokenFor(bob), evidence("Bob"), key));
    assertNotEquals(aliceItem, bobItem);

    for (String bad : List.of("short", "has spaces in the key", "x".repeat(101))) {
      assertProblem(callWithKey("POST", "/evidence", tokenFor(alice), evidence("Nope"), bad), 400);
    }
    assertEquals(1, evidenceCount(alice));

    assertEquals(204, call("DELETE", "/evidence/" + aliceItem, tokenFor(alice), null).statusCode());
    HttpResponse<String> again = callWithKey("POST", "/evidence", tokenFor(alice), evidence("Alice"), key);
    assertEquals(201, again.statusCode(), again.body());
    assertNotEquals(aliceItem, idOf(again));
    assertEquals(1, evidenceCount(alice));
  }

  @Test
  void theDatabaseRefusesEvidenceThatPointsAtAnotherUsersSession() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID aliceSession = startSession(alice);
    assertEquals(200, call("GET", "/me", tokenFor(bob), null).statusCode()); // creates Bob's profile
    try (Connection c = admin();
        var s = c.prepareStatement("insert into public.evidence (user_id, session_id, type, activity_level, "
            + "title) values (?, ?, 'note', 'built', 'x')")) {
      s.setObject(1, bob);
      s.setObject(2, aliceSession);
      assertThrows(SQLException.class, s::executeUpdate);
    }
    assertEquals(0, evidenceCount(bob));
  }
}