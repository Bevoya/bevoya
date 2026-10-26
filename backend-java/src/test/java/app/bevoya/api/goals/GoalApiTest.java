package app.bevoya.api.goals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bevoya.api.identity.ProfileService;
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
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Full stack: real tokens, real migrations, and the app connected as bevoya_app (not the admin),
 * so grants and RLS are in play. Needs Docker: skipped on the dev laptop, runs in CI.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class GoalApiTest {

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

  /** Calls the real ProfileService inside a transaction, then fails. */
  static class TxProbe {
    private final ProfileService profiles;

    TxProbe(ProfileService profiles) {
      this.profiles = profiles;
    }

    @Transactional
    public void ensureThenFail(UUID id) {
      profiles.ensureExists(id);
      throw new IllegalStateException("probe");
    }
  }

  @TestConfiguration
  static class ProbeConfig {
    @Bean
    TxProbe txProbe(ProfileService profiles) {
      return new TxProbe(profiles);
    }
  }

  @Autowired
  TxProbe probe;

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
    createAuthUser(id);
    return id;
  }

  private static void createAuthUser(UUID id) throws SQLException {
    try (Connection c = admin(); var s = c.prepareStatement("insert into auth.users (id) values (?)")) {
      s.setObject(1, id);
      s.executeUpdate();
    }
  }

  private static long profileCount(UUID id) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("select count(*) from public.profiles where id = ?")) {
      s.setObject(1, id);
      try (ResultSet rs = s.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
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

  private static String goalJson(String title) {
    return "{\"title\":\"" + title + "\"}";
  }

  private static UUID idOf(HttpResponse<String> response) {
    Matcher m = ID.matcher(response.body());
    assertTrue(m.find(), response.body());
    return UUID.fromString(m.group(1));
  }

  private static String field(String body, String name) {
    Matcher m = Pattern.compile("\"" + name + "\":(\"[^\"]*\"|[0-9.\\-]+)").matcher(body);
    assertTrue(m.find(), body);
    return m.group(1);
  }

  /**
   * Drops the two fields that legitimately differ per request: the request id, and the
   * "instance" path that Spring MVC fills in with the URI the caller asked for. Each echoes only
   * what the caller sent, so neither says whether the record exists.
   */
  private static String withoutPerRequestFields(String body) {
    return body.replaceAll("\"(requestId|instance)\":\"[^\"]*\",?", "");
  }

  private static void assertProblem(HttpResponse<String> response, int status) {
    assertEquals(status, response.statusCode(), response.body());
    assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/problem+json"));
  }

  private UUID createGoal(UUID user, String title) throws Exception {
    HttpResponse<String> created = call("POST", "/goals", tokenFor(user), goalJson(title));
    assertEquals(201, created.statusCode(), created.body());
    return idOf(created);
  }

  // ---- tests ----

  @Test
  void createdGoalCanBeReadBackAndListed() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = call("POST", "/goals", tokenFor(alice), goalJson("Learn Spanish"));
    assertEquals(201, created.statusCode(), created.body());
    UUID id = idOf(created);
    assertEquals("/goals/" + id, created.headers().firstValue("location").orElse(""));
    assertTrue(created.body().contains("\"status\":\"active\""), created.body());

    HttpResponse<String> fetched = call("GET", "/goals/" + id, tokenFor(alice), null);
    assertEquals(200, fetched.statusCode());
    assertTrue(fetched.body().contains("Learn Spanish"));

    HttpResponse<String> list = call("GET", "/goals", tokenFor(alice), null);
    assertEquals(200, list.statusCode());
    assertTrue(list.body().contains(id.toString()));
  }

  @Test
  void firstGoalCreatesTheProfileWithoutCallingMe() throws Exception {
    UUID alice = signedUpUser();
    assertEquals(0, profileCount(alice));
    createGoal(alice, "First");
    assertEquals(1, profileCount(alice));
    createGoal(alice, "Second");
    assertEquals(1, profileCount(alice));
  }

  @Test
  void anotherUsersGoalIsIndistinguishableFromAMissingOne() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID goal = createGoal(alice, "Alice's goal");
    UUID missing = UUID.randomUUID();
    String update = "{\"title\":\"Hijacked\",\"status\":\"archived\"}";

    for (String[] call : new String[][] {{"GET", null}, {"PUT", update}, {"DELETE", null}}) {
      HttpResponse<String> foreign = call(call[0], "/goals/" + goal, tokenFor(bob), call[1]);
      HttpResponse<String> absent = call(call[0], "/goals/" + missing, tokenFor(bob), call[1]);
      assertProblem(foreign, 404);
      assertProblem(absent, 404);
      assertEquals(withoutPerRequestFields(absent.body()), withoutPerRequestFields(foreign.body()), call[0]);
    }

    HttpResponse<String> still = call("GET", "/goals/" + goal, tokenFor(alice), null);
    assertEquals(200, still.statusCode());
    assertTrue(still.body().contains("Alice's goal"));
    assertTrue(still.body().contains("\"status\":\"active\""));
  }

  @Test
  void listsNeverCrossUsersAndABodyUserIdIsNotTrusted() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID aliceGoal = createGoal(alice, "Alice only");

    HttpResponse<String> bobList = call("GET", "/goals", tokenFor(bob), null);
    assertEquals(200, bobList.statusCode());
    assertEquals("[]", bobList.body());

    // Bob tries to create a goal "for" Alice. Either it is rejected or it becomes Bob's.
    HttpResponse<String> sneaky = call("POST", "/goals", tokenFor(bob),
        "{\"title\":\"Sneaky\",\"userId\":\"" + alice + "\"}");
    assertTrue(sneaky.statusCode() == 201 || sneaky.statusCode() == 400, sneaky.body());
    if (sneaky.statusCode() == 201) {
      UUID sneakyId = idOf(sneaky);
      assertEquals(404, call("GET", "/goals/" + sneakyId, tokenFor(alice), null).statusCode());
      assertEquals(200, call("GET", "/goals/" + sneakyId, tokenFor(bob), null).statusCode());
    }
    HttpResponse<String> aliceList = call("GET", "/goals", tokenFor(alice), null);
    assertTrue(aliceList.body().contains(aliceGoal.toString()));
    assertTrue(!aliceList.body().contains("Sneaky"));
  }

  @Test
  void putReplacesTheGoalAndMovesUpdatedAt() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = call("POST", "/goals", tokenFor(alice), goalJson("Draft"));
    UUID id = idOf(created);
    String createdAt = field(created.body(), "createdAt");
    String updatedBefore = field(created.body(), "updatedAt");

    HttpResponse<String> replaced = call("PUT", "/goals/" + id, tokenFor(alice),
        "{\"title\":\"Final title\",\"description\":\"Why it matters\",\"status\":\"paused\","
            + "\"targetDate\":\"2027-01-31\"}");
    assertEquals(200, replaced.statusCode(), replaced.body());
    assertTrue(replaced.body().contains("Final title"));
    assertTrue(replaced.body().contains("\"status\":\"paused\""));
    assertTrue(replaced.body().contains("2027-01-31"), replaced.body());
    assertEquals(createdAt, field(replaced.body(), "createdAt"));
    assertNotEquals(updatedBefore, field(replaced.body(), "updatedAt"));

    HttpResponse<String> fetched = call("GET", "/goals/" + id, tokenFor(alice), null);
    assertTrue(fetched.body().contains("Final title"), "the update must be persisted, not only returned");
    assertTrue(fetched.body().contains("Why it matters"));
  }

  @Test
  void statusFilterOnlyReturnsMatchingGoals() throws Exception {
    UUID alice = signedUpUser();
    UUID active = createGoal(alice, "Stays active");
    UUID paused = createGoal(alice, "Gets paused");
    assertEquals(200, call("PUT", "/goals/" + paused, tokenFor(alice),
        "{\"title\":\"Gets paused\",\"status\":\"paused\"}").statusCode());

    String onlyPaused = call("GET", "/goals?status=paused", tokenFor(alice), null).body();
    assertTrue(onlyPaused.contains(paused.toString()));
    assertTrue(!onlyPaused.contains(active.toString()));
  }

  @Test
  void deleteRemovesTheGoalOnce() throws Exception {
    UUID alice = signedUpUser();
    UUID id = createGoal(alice, "Short lived");
    HttpResponse<String> deleted = call("DELETE", "/goals/" + id, tokenFor(alice), null);
    assertEquals(204, deleted.statusCode());
    assertEquals("", deleted.body());
    assertEquals(404, call("GET", "/goals/" + id, tokenFor(alice), null).statusCode());
    assertEquals(404, call("DELETE", "/goals/" + id, tokenFor(alice), null).statusCode());
  }

  @Test
  void invalidInputIsRejectedWithAProblem() throws Exception {
    UUID alice = signedUpUser();
    String token = tokenFor(alice);
    List<String> badBodies = List.of(
        "{\"title\":\"\"}",
        "{\"title\":\"   \"}",
        "{}",
        goalJson("x".repeat(201)),
        "{\"title\":\"ok\",\"status\":\"nope\"}",
        "{\"title\":\"ok\",\"description\":\"" + "d".repeat(2001) + "\"}",
        "{not json");
    for (String body : badBodies) {
      assertProblem(call("POST", "/goals", token, body), 400);
    }
    UUID id = createGoal(alice, "Valid");
    assertProblem(call("PUT", "/goals/" + id, token, goalJson("No status")), 400);
    assertProblem(call("GET", "/goals?status=nope", token, null), 400);
    assertProblem(call("GET", "/goals/not-a-uuid", token, null), 400);
  }

  @Test
  void missingTokenIsRejectedOnEveryRoute() throws Exception {
    String id = UUID.randomUUID().toString();
    String json = "{\"title\":\"x\",\"status\":\"active\"}";
    String[][] routes = {
        {"POST", "/goals"}, {"GET", "/goals"}, {"GET", "/goals/" + id},
        {"PUT", "/goals/" + id}, {"DELETE", "/goals/" + id}};
    for (String[] route : routes) {
      boolean hasBody = route[0].equals("POST") || route[0].equals("PUT");
      HttpResponse<String> response = call(route[0], route[1], null, hasBody ? json : null);
      assertProblem(response, 401);
      assertTrue(response.headers().firstValue("www-authenticate").orElse("").startsWith("Bearer"));
    }
  }

  @Test
  void tokenOfADeletedAccountIsRefusedOnCreate() throws Exception {
    UUID ghost = UUID.randomUUID(); // valid token, no auth.users row
    HttpResponse<String> response = call("POST", "/goals", tokenFor(ghost), goalJson("Ghost"));
    assertProblem(response, 401);
    assertTrue(response.headers().firstValue("www-authenticate").orElse("").startsWith("Bearer"));
    assertEquals(0, profileCount(ghost));
  }

  @Test
  void profileInsertRollsBackWithTheSurroundingTransaction() throws Exception {
    UUID id = signedUpUser();
    assertThrows(IllegalStateException.class, () -> probe.ensureThenFail(id));
    assertEquals(0, profileCount(id),
        "ensureExists (JDBC) must join the JPA transaction, or a failed goal insert leaves a profile behind");
  }
}