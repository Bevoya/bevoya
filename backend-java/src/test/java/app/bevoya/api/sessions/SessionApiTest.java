package app.bevoya.api.sessions;

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
import java.time.LocalDate;
import java.time.ZoneId;
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

/** Focus sessions against a real database, as bevoya_app, with real tokens. Needs Docker (CI). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class SessionApiTest {

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

  private static long sessionCount(UUID user) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("select count(*) from public.sessions where user_id = ?")) {
      s.setObject(1, user);
      try (ResultSet rs = s.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private static void backdate(UUID session, int minutes) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement(
            "update public.sessions set started_at = now() - make_interval(mins => ?) where id = ?")) {
      s.setInt(1, minutes);
      s.setObject(2, session);
      assertEquals(1, s.executeUpdate());
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

  private static String session(String activity) {
    return "{\"activityType\":\"" + activity + "\"}";
  }

  private HttpResponse<String> start(UUID user, String json) throws Exception {
    return call("POST", "/sessions", tokenFor(user), json);
  }

  private UUID startOk(UUID user, String json) throws Exception {
    HttpResponse<String> started = start(user, json);
    assertEquals(201, started.statusCode(), started.body());
    return idOf(started);
  }

  private HttpResponse<String> complete(UUID user, UUID id, String json) throws Exception {
    return call("POST", "/sessions/" + id + "/complete", tokenFor(user), json);
  }

  private UUID createTask(UUID user, String title) throws Exception {
    HttpResponse<String> created = call("POST", "/tasks", tokenFor(user),
        "{\"title\":\"" + title + "\",\"category\":\"learn\"}");
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
  void startReturnsARunningSessionWithAServerStartTime() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> started = start(alice, "{\"activityType\":\"build\",\"topic\":\"  RAG endpoint  \"}");
    assertEquals(201, started.statusCode(), started.body());
    UUID id = idOf(started);
    assertEquals("/sessions/" + id, started.headers().firstValue("location").orElse(""));
    assertEquals("\"build\"", field(started.body(), "activityType"));
    assertEquals("\"  RAG endpoint  \"", field(started.body(), "topic"));
    assertNotEquals("null", field(started.body(), "startedAt"));
    assertEquals("null", field(started.body(), "endedAt"));
    assertEquals("null", field(started.body(), "actualMinutes"));
    assertEquals("null", field(started.body(), "taskId"));

    HttpResponse<String> read = call("GET", "/sessions/" + id, tokenFor(alice), null);
    assertEquals(200, read.statusCode(), read.body());
    assertEquals(started.body(), read.body());
  }

  @Test
  void aSecondRunningSessionIsRefusedUntilTheFirstIsCompleted() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID first = startOk(alice, session("study"));

    HttpResponse<String> second = start(alice, session("build"));
    assertProblem(second, 409);
    assertEquals("\"" + first + "\"", field(second.body(), "activeSessionId"));
    assertEquals(1, sessionCount(alice));

    // another person can run their own session at the same time
    startOk(bob, session("study"));

    assertEquals(200, complete(alice, first, null).statusCode());
    startOk(alice, session("build"));
    assertEquals(2, sessionCount(alice));
  }

  @Test
  void completeSetsTheEndTimeAndWholeMinutesFromTheStartTime() throws Exception {
    UUID alice = signedUpUser();
    UUID id = startOk(alice, session("practice"));
    backdate(id, 25);

    HttpResponse<String> done = complete(alice, id,
        "{\"focusScore\":4,\"output\":\"Solved 10 questions\",\"notes\":\"Integration by parts is hard\"}");
    assertEquals(200, done.statusCode(), done.body());
    assertNotEquals("null", field(done.body(), "endedAt"));
    assertEquals("25", field(done.body(), "actualMinutes"));
    assertEquals("4", field(done.body(), "focusScore"));
    assertEquals("\"Solved 10 questions\"", field(done.body(), "output"));

    HttpResponse<String> reread = call("GET", "/sessions/" + id, tokenFor(alice), null);
    assertEquals(done.body(), reread.body(), "the completion must be persisted, not only returned");
  }

  @Test
  void completeWorksWithoutABody() throws Exception {
    UUID alice = signedUpUser();
    UUID id = startOk(alice, session("read"));
    HttpResponse<String> done = complete(alice, id, null);
    assertEquals(200, done.statusCode(), done.body());
    assertEquals("0", field(done.body(), "actualMinutes"));
    assertEquals("null", field(done.body(), "focusScore"));
  }

  @Test
  void completingTwiceIsAConflictAndKeepsTheFirstResult() throws Exception {
    UUID alice = signedUpUser();
    UUID id = startOk(alice, session("work"));
    backdate(id, 10);
    HttpResponse<String> first = complete(alice, id, "{\"focusScore\":5}");
    assertEquals(200, first.statusCode(), first.body());

    assertProblem(complete(alice, id, "{\"focusScore\":1}"), 409);
    HttpResponse<String> reread = call("GET", "/sessions/" + id, tokenFor(alice), null);
    assertEquals(first.body(), reread.body());
  }

  @Test
  void aSessionThatIsMissingAndASessionThatIsNotYoursGiveTheSame404() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID session = startOk(alice, session("study"));
    UUID missing = UUID.randomUUID();

    String[][] calls = {{"GET", "", null}, {"POST", "/complete", "{\"focusScore\":3}"}, {"DELETE", "", null}};
    for (String[] c : calls) {
      HttpResponse<String> foreign = call(c[0], "/sessions/" + session + c[1], tokenFor(bob), c[2]);
      HttpResponse<String> absent = call(c[0], "/sessions/" + missing + c[1], tokenFor(bob), c[2]);
      assertProblem(foreign, 404);
      assertProblem(absent, 404);
      assertEquals(withoutPerRequestFields(absent.body()), withoutPerRequestFields(foreign.body()), c[0]);
    }

    HttpResponse<String> mine = call("GET", "/sessions/" + session, tokenFor(alice), null);
    assertEquals(200, mine.statusCode(), mine.body());
    assertEquals("null", field(mine.body(), "endedAt"), "Bob must not have completed Alice's session");
  }

  @Test
  void activeReturnsTheRunningSessionOr204AndNeverAnotherUsers() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    assertEquals(204, call("GET", "/sessions/active", tokenFor(alice), null).statusCode());

    UUID id = startOk(alice, session("study"));
    HttpResponse<String> active = call("GET", "/sessions/active", tokenFor(alice), null);
    assertEquals(200, active.statusCode(), active.body());
    assertEquals(id, idOf(active));
    assertEquals(204, call("GET", "/sessions/active", tokenFor(bob), null).statusCode());

    assertEquals(200, complete(alice, id, null).statusCode());
    assertEquals(204, call("GET", "/sessions/active", tokenFor(alice), null).statusCode());
  }

  @Test
  void listsOnlyYourOwnSessionsNewestFirstAndFiltersByDate() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID old = startOk(alice, session("study"));
    backdate(old, 10 * 24 * 60);
    assertEquals(200, complete(alice, old, null).statusCode());
    UUID recent = startOk(alice, session("build"));
    startOk(bob, session("study"));

    assertEquals(List.of(recent, old), idsOf("/sessions", alice));
    assertEquals(1, idsOf("/sessions", bob).size());
    LocalDate since = LocalDate.now(ZoneId.of("UTC")).minusDays(2);
    assertEquals(List.of(recent), idsOf("/sessions?from=" + since, alice));
    assertProblem(call("GET", "/sessions?from=yesterday", tokenFor(alice), null), 400);
  }

  @Test
  void aSessionCanOnlyLinkToYourOwnTask() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID aliceTask = createTask(alice, "Alice's task");

    assertProblem(start(bob, "{\"activityType\":\"study\",\"taskId\":\"" + aliceTask + "\"}"), 404);
    assertProblem(start(bob, "{\"activityType\":\"study\",\"taskId\":\"" + UUID.randomUUID() + "\"}"), 404);
    assertEquals(0, sessionCount(bob));

    HttpResponse<String> linked = start(alice, "{\"activityType\":\"study\",\"taskId\":\"" + aliceTask + "\"}");
    assertEquals(201, linked.statusCode(), linked.body());
    assertEquals("\"" + aliceTask + "\"", field(linked.body(), "taskId"));
  }

  @Test
  void deletingATaskKeepsItsSessionButUnlinksIt() throws Exception {
    UUID alice = signedUpUser();
    UUID task = createTask(alice, "Temporary task");
    UUID id = startOk(alice, "{\"activityType\":\"study\",\"taskId\":\"" + task + "\"}");

    assertEquals(204, call("DELETE", "/tasks/" + task, tokenFor(alice), null).statusCode());
    HttpResponse<String> read = call("GET", "/sessions/" + id, tokenFor(alice), null);
    assertEquals(200, read.statusCode(), read.body());
    assertEquals("null", field(read.body(), "taskId"));
  }

  @Test
  void deleteRemovesASessionOnceAndFreesTheRunningSlot() throws Exception {
    UUID alice = signedUpUser();
    UUID id = startOk(alice, session("study"));
    assertEquals(204, call("DELETE", "/sessions/" + id, tokenFor(alice), null).statusCode());
    assertProblem(call("DELETE", "/sessions/" + id, tokenFor(alice), null), 404);
    assertProblem(call("GET", "/sessions/" + id, tokenFor(alice), null), 404);
    assertEquals(0, sessionCount(alice));
    startOk(alice, session("study"));
  }

  @Test
  void invalidInputIsRejectedWithAProblemAndCreatesNothing() throws Exception {
    UUID alice = signedUpUser();
    List<String> badBodies = List.of(
        "{}",
        "{\"topic\":\"no activity\"}",
        "{\"activityType\":\"\"}",
        "{\"activityType\":\"   \"}",
        "{\"activityType\":\"nope\"}",
        "{\"activityType\":\"study\",\"topic\":\"" + "t".repeat(201) + "\"}",
        "{\"activityType\":\"study\",\"taskId\":\"not-a-uuid\"}",
        "{not json");
    for (String body : badBodies) {
      assertProblem(start(alice, body), 400);
    }
    assertEquals(0, sessionCount(alice));

    UUID id = startOk(alice, session("study"));
    List<String> badCompletions = List.of(
        "{\"focusScore\":0}",
        "{\"focusScore\":6}",
        "{\"output\":\"" + "o".repeat(5001) + "\"}",
        "{\"notes\":\"" + "n".repeat(10001) + "\"}",
        "{not json");
    for (String body : badCompletions) {
      assertProblem(complete(alice, id, body), 400);
    }
    assertProblem(call("GET", "/sessions/not-a-uuid", tokenFor(alice), null), 400);
    assertEquals("null", field(call("GET", "/sessions/" + id, tokenFor(alice), null).body(), "endedAt"));
  }

  @Test
  void missingTokenIsRejectedOnEveryRoute() throws Exception {
    String id = UUID.randomUUID().toString();
    String[][] routes = {
        {"POST", "/sessions", session("study")},
        {"POST", "/sessions/" + id + "/complete", null},
        {"GET", "/sessions", null},
        {"GET", "/sessions/active", null},
        {"GET", "/sessions/" + id, null},
        {"DELETE", "/sessions/" + id, null}};
    for (String[] route : routes) {
      HttpResponse<String> response = call(route[0], route[1], null, route[2]);
      assertProblem(response, 401);
      assertTrue(response.headers().firstValue("www-authenticate").orElse("").startsWith("Bearer"));
    }
  }

  @Test
  void sameKeyTwiceStartsOneSessionAndReturnsTheOriginalEvenAfterItIsCompleted() throws Exception {
    UUID alice = signedUpUser();
    String key = UUID.randomUUID().toString();
    HttpResponse<String> first = callWithKey("POST", "/sessions", tokenFor(alice), session("study"), key);
    HttpResponse<String> retry = callWithKey("POST", "/sessions", tokenFor(alice), session("build"), key);
    assertEquals(201, first.statusCode(), first.body());
    assertEquals(201, retry.statusCode(), retry.body());
    assertEquals(idOf(first), idOf(retry));
    assertEquals(first.body(), retry.body());
    assertEquals(1, sessionCount(alice));

    assertEquals(200, complete(alice, idOf(first), null).statusCode());
    HttpResponse<String> late = callWithKey("POST", "/sessions", tokenFor(alice), session("study"), key);
    assertEquals(201, late.statusCode(), late.body());
    assertEquals(idOf(first), idOf(late));
    assertEquals(1, sessionCount(alice));
  }

  @Test
  void keysAreScopedPerUserAndMalformedKeysAreRefused() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    String key = UUID.randomUUID().toString();
    UUID aliceSession = idOf(callWithKey("POST", "/sessions", tokenFor(alice), session("study"), key));
    UUID bobSession = idOf(callWithKey("POST", "/sessions", tokenFor(bob), session("study"), key));
    assertNotEquals(aliceSession, bobSession);

    for (String bad : List.of("short", "has spaces in the key", "x".repeat(101))) {
      assertProblem(callWithKey("POST", "/sessions", tokenFor(alice), session("study"), bad), 400);
    }
    assertEquals(1, sessionCount(alice));
  }

  @Test
  void theDatabaseRefusesASessionThatPointsAtAnotherUsersTask() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID aliceTask = createTask(alice, "Alice's task");
    assertEquals(200, call("GET", "/me", tokenFor(bob), null).statusCode()); // creates Bob's profile
    try (Connection c = admin();
        var s = c.prepareStatement(
            "insert into public.sessions (user_id, task_id, activity_type) values (?, ?, 'study')")) {
      s.setObject(1, bob);
      s.setObject(2, aliceTask);
      assertThrows(SQLException.class, s::executeUpdate);
    }
    assertEquals(0, sessionCount(bob));
  }

  @Test
  void theDatabaseRefusesTwoRunningSessionsForOneUser() throws Exception {
    UUID alice = signedUpUser();
    startOk(alice, session("study"));
    try (Connection c = admin();
        var s = c.prepareStatement(
            "insert into public.sessions (user_id, activity_type) values (?, 'build')")) {
      s.setObject(1, alice);
      assertThrows(SQLException.class, s::executeUpdate);
    }
    assertEquals(1, sessionCount(alice));
  }
}