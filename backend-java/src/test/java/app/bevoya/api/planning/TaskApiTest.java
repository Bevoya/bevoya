package app.bevoya.api.planning;

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

/** Tasks and today's plan against a real database, as bevoya_app, with real tokens. Needs Docker (CI). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class TaskApiTest {

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

  private static long taskCount(UUID user) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("select count(*) from public.tasks where user_id = ?")) {
      s.setObject(1, user);
      try (ResultSet rs = s.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private static void setTimeZone(UUID user, String zone) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("update public.profiles set time_zone = ? where id = ?")) {
      s.setString(1, zone);
      s.setObject(2, user);
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
    assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/problem+json"));
  }

  private static String task(String title) {
    return "{\"title\":\"" + title + "\",\"category\":\"learn\"}";
  }

  private HttpResponse<String> postTask(UUID user, String json) throws Exception {
    return call("POST", "/tasks", tokenFor(user), json);
  }

  private UUID createTask(UUID user, String json) throws Exception {
    HttpResponse<String> created = postTask(user, json);
    assertEquals(201, created.statusCode(), created.body());
    return idOf(created);
  }

  private UUID createGoal(UUID user, String title) throws Exception {
    HttpResponse<String> created = call("POST", "/goals", tokenFor(user), "{\"title\":\"" + title + "\"}");
    assertEquals(201, created.statusCode(), created.body());
    return idOf(created);
  }

  private UUID createMilestone(UUID user, UUID goal, String title) throws Exception {
    HttpResponse<String> created = call("POST", "/goals/" + goal + "/milestones", tokenFor(user),
        "{\"title\":\"" + title + "\"}");
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
  void createReturnsTheTaskWithDefaultsAndItIsReadableAgain() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = postTask(alice, "{\"title\":\"  Read chapter 4  \",\"category\":\"revise\"}");
    assertEquals(201, created.statusCode(), created.body());
    UUID id = idOf(created);
    assertEquals("/tasks/" + id, created.headers().firstValue("location").orElse(""));
    assertEquals("\"Read chapter 4\"", field(created.body(), "title"));
    assertEquals("\"revise\"", field(created.body(), "category"));
    assertEquals("\"todo\"", field(created.body(), "status"));
    assertEquals("2", field(created.body(), "priority"));
    assertEquals("null", field(created.body(), "completedAt"));
    assertEquals("null", field(created.body(), "goalId"));

    HttpResponse<String> read = call("GET", "/tasks/" + id, tokenFor(alice), null);
    assertEquals(200, read.statusCode(), read.body());
    assertEquals(created.body(), read.body());
  }

  @Test
  void aTaskCreatedAsDoneHasACompletionTime() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = postTask(alice,
        "{\"title\":\"Already done\",\"category\":\"work\",\"status\":\"done\"}");
    assertEquals(201, created.statusCode(), created.body());
    assertNotEquals("null", field(created.body(), "completedAt"));
  }

  @Test
  void aTaskThatIsMissingAndATaskThatIsNotYoursGiveTheSame404() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID task = createTask(alice, task("Alice's task"));
    UUID missing = UUID.randomUUID();
    String patch = "{\"status\":\"done\",\"title\":\"Hijacked\"}";

    String[][] calls = {{"GET", null}, {"PATCH", patch}, {"DELETE", null}};
    for (String[] c : calls) {
      HttpResponse<String> foreign = call(c[0], "/tasks/" + task, tokenFor(bob), c[1]);
      HttpResponse<String> absent = call(c[0], "/tasks/" + missing, tokenFor(bob), c[1]);
      assertProblem(foreign, 404);
      assertProblem(absent, 404);
      assertEquals(withoutPerRequestFields(absent.body()), withoutPerRequestFields(foreign.body()), c[0]);
    }

    HttpResponse<String> mine = call("GET", "/tasks/" + task, tokenFor(alice), null);
    assertEquals(200, mine.statusCode(), mine.body());
    assertEquals("\"Alice's task\"", field(mine.body(), "title"));
    assertEquals("\"todo\"", field(mine.body(), "status"));
  }

  @Test
  void listsOnlyYourOwnTasksInPlanOrder() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID low = createTask(alice,
        "{\"title\":\"Low\",\"category\":\"learn\",\"priority\":3,\"plannedFor\":\"2027-03-01\"}");
    UUID high = createTask(alice,
        "{\"title\":\"High\",\"category\":\"learn\",\"priority\":1,\"plannedFor\":\"2027-03-01\"}");
    UUID undated = createTask(alice, task("Undated"));
    createTask(bob, task("Bob's"));

    assertEquals(List.of(high, low, undated), idsOf("/tasks", alice));
    assertEquals(1, idsOf("/tasks", bob).size());
  }

  @Test
  void filtersByDateStatusAndGoal() throws Exception {
    UUID alice = signedUpUser();
    UUID goal = createGoal(alice, "Goal");
    UUID onGoal = createTask(alice,
        "{\"title\":\"On goal\",\"category\":\"build\",\"plannedFor\":\"2027-03-01\",\"goalId\":\"" + goal + "\"}");
    UUID other = createTask(alice,
        "{\"title\":\"Other\",\"category\":\"build\",\"plannedFor\":\"2027-03-02\"}");
    assertEquals(200, call("PATCH", "/tasks/" + other, tokenFor(alice), "{\"status\":\"done\"}").statusCode());

    assertEquals(List.of(onGoal), idsOf("/tasks?date=2027-03-01", alice));
    assertEquals(List.of(other), idsOf("/tasks?status=done", alice));
    assertEquals(List.of(onGoal), idsOf("/tasks?goalId=" + goal, alice));
    assertEquals(List.of(), idsOf("/tasks?goalId=" + UUID.randomUUID(), alice));

    assertProblem(call("GET", "/tasks?status=nope", tokenFor(alice), null), 400);
    assertProblem(call("GET", "/tasks?date=tomorrow", tokenFor(alice), null), 400);
    assertProblem(call("GET", "/tasks?goalId=not-a-uuid", tokenFor(alice), null), 400);
  }

  @Test
  void patchChangesOnlyTheSentFieldsAndTracksCompletion() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = postTask(alice,
        "{\"title\":\"Draft\",\"description\":\"Keep me\",\"category\":\"create\",\"priority\":3}");
    UUID id = idOf(created);

    HttpResponse<String> done = call("PATCH", "/tasks/" + id, tokenFor(alice),
        "{\"title\":\"  Final  \",\"status\":\"done\"}");
    assertEquals(200, done.statusCode(), done.body());
    assertEquals("\"Final\"", field(done.body(), "title"));
    assertEquals("\"Keep me\"", field(done.body(), "description"));
    assertEquals("\"create\"", field(done.body(), "category"));
    assertEquals("3", field(done.body(), "priority"));
    assertEquals("\"done\"", field(done.body(), "status"));
    assertNotEquals("null", field(done.body(), "completedAt"));
    assertEquals(field(created.body(), "createdAt"), field(done.body(), "createdAt"));
    assertNotEquals(field(created.body(), "updatedAt"), field(done.body(), "updatedAt"));

    HttpResponse<String> again = call("PATCH", "/tasks/" + id, tokenFor(alice), "{\"status\":\"done\"}");
    assertEquals(field(done.body(), "completedAt"), field(again.body(), "completedAt"),
        "completing a done task again keeps the first completion time");

    HttpResponse<String> reopened = call("PATCH", "/tasks/" + id, tokenFor(alice), "{\"status\":\"doing\"}");
    assertEquals("\"doing\"", field(reopened.body(), "status"));
    assertEquals("null", field(reopened.body(), "completedAt"));

    HttpResponse<String> reread = call("GET", "/tasks/" + id, tokenFor(alice), null);
    assertEquals(reopened.body(), reread.body(), "the update must be persisted, not only returned");
  }

  @Test
  void aTaskCanOnlyLinkToYourOwnGoalAndMilestone() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID aliceGoal = createGoal(alice, "Alice's goal");
    UUID aliceMilestone = createMilestone(alice, aliceGoal, "Alice's milestone");
    UUID bobGoal = createGoal(bob, "Bob's goal");
    UUID bobMilestone = createMilestone(bob, bobGoal, "Bob's milestone");
    UUID otherGoal = createGoal(alice, "Another goal of Alice");

    // someone else's goal or milestone, a missing goal: all refused with 404, nothing created
    assertProblem(postTask(bob, "{\"title\":\"x\",\"category\":\"learn\",\"goalId\":\"" + aliceGoal + "\"}"), 404);
    assertProblem(postTask(bob,
        "{\"title\":\"x\",\"category\":\"learn\",\"milestoneId\":\"" + aliceMilestone + "\"}"), 404);
    assertProblem(postTask(bob, "{\"title\":\"x\",\"category\":\"learn\",\"goalId\":\""
        + UUID.randomUUID() + "\"}"), 404);
    // a milestone of a different goal than the one given
    assertProblem(postTask(alice, "{\"title\":\"x\",\"category\":\"learn\",\"goalId\":\"" + otherGoal
        + "\",\"milestoneId\":\"" + aliceMilestone + "\"}"), 404);
    assertEquals(0, taskCount(bob));
    assertEquals(0, taskCount(alice));

    // your own goal and milestone work
    HttpResponse<String> linked = postTask(alice, "{\"title\":\"Linked\",\"category\":\"learn\",\"goalId\":\""
        + aliceGoal + "\",\"milestoneId\":\"" + aliceMilestone + "\"}");
    assertEquals(201, linked.statusCode(), linked.body());
    assertEquals("\"" + aliceGoal + "\"", field(linked.body(), "goalId"));
    assertEquals("\"" + aliceMilestone + "\"", field(linked.body(), "milestoneId"));

    // moving a task to someone else's goal is refused and changes nothing
    UUID mine = createTask(bob, task("Bob's task"));
    assertProblem(call("PATCH", "/tasks/" + mine, tokenFor(bob),
        "{\"goalId\":\"" + aliceGoal + "\"}"), 404);
    assertProblem(call("PATCH", "/tasks/" + mine, tokenFor(bob),
        "{\"milestoneId\":\"" + aliceMilestone + "\"}"), 404);
    assertEquals("null", field(call("GET", "/tasks/" + mine, tokenFor(bob), null).body(), "goalId"));
    // and moving it to its own goal works
    HttpResponse<String> moved = call("PATCH", "/tasks/" + mine, tokenFor(bob),
        "{\"goalId\":\"" + bobGoal + "\",\"milestoneId\":\"" + bobMilestone + "\"}");
    assertEquals(200, moved.statusCode(), moved.body());
  }

  @Test
  void deletingAGoalKeepsItsTasksButUnlinksThem() throws Exception {
    UUID alice = signedUpUser();
    UUID goal = createGoal(alice, "Goal");
    UUID milestone = createMilestone(alice, goal, "Milestone");
    UUID id = createTask(alice, "{\"title\":\"Linked\",\"category\":\"learn\",\"goalId\":\"" + goal
        + "\",\"milestoneId\":\"" + milestone + "\"}");

    assertEquals(204, call("DELETE", "/goals/" + goal, tokenFor(alice), null).statusCode());
    HttpResponse<String> read = call("GET", "/tasks/" + id, tokenFor(alice), null);
    assertEquals(200, read.statusCode(), read.body());
    assertEquals("null", field(read.body(), "goalId"));
    assertEquals("null", field(read.body(), "milestoneId"));
  }

  @Test
  void deleteRemovesATaskOnce() throws Exception {
    UUID alice = signedUpUser();
    UUID id = createTask(alice, task("Temporary"));
    assertEquals(204, call("DELETE", "/tasks/" + id, tokenFor(alice), null).statusCode());
    assertProblem(call("DELETE", "/tasks/" + id, tokenFor(alice), null), 404);
    assertProblem(call("GET", "/tasks/" + id, tokenFor(alice), null), 404);
    assertEquals(0, taskCount(alice));
  }

  @Test
  void invalidInputIsRejectedWithAProblemAndCreatesNothing() throws Exception {
    UUID alice = signedUpUser();
    List<String> badBodies = List.of(
        "{}",
        "{\"category\":\"learn\"}",
        "{\"title\":\"ok\"}",
        "{\"title\":\"\",\"category\":\"learn\"}",
        "{\"title\":\"   \",\"category\":\"learn\"}",
        "{\"title\":\"" + "x".repeat(201) + "\",\"category\":\"learn\"}",
        "{\"title\":\"ok\",\"category\":\"nope\"}",
        "{\"title\":\"ok\",\"category\":\"learn\",\"priority\":0}",
        "{\"title\":\"ok\",\"category\":\"learn\",\"priority\":4}",
        "{\"title\":\"ok\",\"category\":\"learn\",\"estimatedMinutes\":0}",
        "{\"title\":\"ok\",\"category\":\"learn\",\"estimatedMinutes\":1441}",
        "{\"title\":\"ok\",\"category\":\"learn\",\"status\":\"nope\"}",
        "{\"title\":\"ok\",\"category\":\"learn\",\"plannedFor\":\"next week\"}",
        "{\"title\":\"ok\",\"category\":\"learn\",\"whyItMatters\":\"" + "w".repeat(501) + "\"}",
        "{\"title\":\"ok\",\"category\":\"learn\",\"description\":\"" + "d".repeat(2001) + "\"}",
        "{not json");
    for (String body : badBodies) {
      assertProblem(postTask(alice, body), 400);
    }
    UUID id = createTask(alice, task("Valid"));
    List<String> badPatches = List.of(
        "{\"title\":\"   \"}",
        "{\"status\":\"nope\"}",
        "{\"priority\":9}",
        "{\"category\":\"nope\"}",
        "{\"plannedFor\":\"soon\"}");
    for (String body : badPatches) {
      assertProblem(call("PATCH", "/tasks/" + id, tokenFor(alice), body), 400);
    }
    assertProblem(call("GET", "/tasks/not-a-uuid", tokenFor(alice), null), 400);
    assertEquals(1, taskCount(alice));
  }

  @Test
  void missingTokenIsRejectedOnEveryRoute() throws Exception {
    String id = UUID.randomUUID().toString();
    String json = "{\"title\":\"x\",\"category\":\"learn\"}";
    String[][] routes = {
        {"POST", "/tasks", json},
        {"GET", "/tasks", null},
        {"GET", "/tasks/" + id, null},
        {"PATCH", "/tasks/" + id, "{\"status\":\"done\"}"},
        {"DELETE", "/tasks/" + id, null},
        {"GET", "/plans/today", null}};
    for (String[] route : routes) {
      HttpResponse<String> response = call(route[0], route[1], null, route[2]);
      assertProblem(response, 401);
      assertTrue(response.headers().firstValue("www-authenticate").orElse("").startsWith("Bearer"));
    }
  }

  @Test
  void sameKeyTwiceCreatesOneTaskAndReturnsTheOriginal() throws Exception {
    UUID alice = signedUpUser();
    String key = UUID.randomUUID().toString();
    HttpResponse<String> first = callWithKey("POST", "/tasks", tokenFor(alice), task("Original"), key);
    HttpResponse<String> retry = callWithKey("POST", "/tasks", tokenFor(alice), task("A different title"), key);
    assertEquals(201, first.statusCode(), first.body());
    assertEquals(201, retry.statusCode(), retry.body());
    assertEquals(idOf(first), idOf(retry));
    assertEquals(first.body(), retry.body(), "a retry returns the original task unchanged");
    assertEquals(1, taskCount(alice));
  }

  @Test
  void keysAreScopedPerUserMalformedKeysAreRefusedAndADeletedTaskReleasesItsKey() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    String key = UUID.randomUUID().toString();
    UUID aliceTask = idOf(callWithKey("POST", "/tasks", tokenFor(alice), task("Alice"), key));
    UUID bobTask = idOf(callWithKey("POST", "/tasks", tokenFor(bob), task("Bob"), key));
    assertNotEquals(aliceTask, bobTask);

    for (String bad : List.of("short", "has spaces in the key", "x".repeat(101))) {
      assertProblem(callWithKey("POST", "/tasks", tokenFor(alice), task("Nope"), bad), 400);
    }
    assertEquals(1, taskCount(alice));

    assertEquals(204, call("DELETE", "/tasks/" + aliceTask, tokenFor(alice), null).statusCode());
    HttpResponse<String> again = callWithKey("POST", "/tasks", tokenFor(alice), task("Alice"), key);
    assertEquals(201, again.statusCode(), again.body());
    assertNotEquals(aliceTask, idOf(again));
    assertEquals(1, taskCount(alice));
  }

  @Test
  void todayUsesTheUsersOwnTimeZoneAndPutsFinishedTasksLast() throws Exception {
    UUID alice = signedUpUser();
    assertEquals(200, call("GET", "/me", tokenFor(alice), null).statusCode()); // creates the profile
    setTimeZone(alice, "Pacific/Kiritimati"); // UTC+14
    LocalDate east = LocalDate.now(ZoneId.of("Pacific/Kiritimati"));
    LocalDate west = LocalDate.now(ZoneId.of("Pacific/Pago_Pago")); // UTC-11: always a different date

    UUID finished = createTask(alice, "{\"title\":\"Finished\",\"category\":\"work\",\"priority\":1,"
        + "\"status\":\"done\",\"plannedFor\":\"" + east + "\"}");
    UUID open = createTask(alice, "{\"title\":\"Open\",\"category\":\"work\",\"priority\":3,"
        + "\"plannedFor\":\"" + east + "\"}");
    UUID otherDay = createTask(alice, "{\"title\":\"Other day\",\"category\":\"work\",\"plannedFor\":\""
        + west + "\"}");
    createTask(alice, task("Undated"));

    assertEquals(List.of(open, finished), idsOf("/plans/today", alice));

    setTimeZone(alice, "Pacific/Pago_Pago");
    assertEquals(List.of(otherDay), idsOf("/plans/today", alice));
  }

  @Test
  void todayShowsNobodyElsesTasks() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    assertEquals(200, call("GET", "/me", tokenFor(alice), null).statusCode());
    assertEquals(200, call("GET", "/me", tokenFor(bob), null).statusCode());
    LocalDate utcToday = LocalDate.now(ZoneId.of("UTC"));
    setTimeZone(alice, "UTC");
    setTimeZone(bob, "UTC");
    createTask(alice, "{\"title\":\"Alice\",\"category\":\"work\",\"plannedFor\":\"" + utcToday + "\"}");

    assertEquals(1, idsOf("/plans/today", alice).size());
    assertEquals(List.of(), idsOf("/plans/today", bob));
  }

  @Test
  void theDatabaseRefusesATaskThatPointsAtAnotherUsersGoal() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID aliceGoal = createGoal(alice, "Alice's goal");
    assertEquals(200, call("GET", "/me", tokenFor(bob), null).statusCode()); // creates Bob's profile
    try (Connection c = admin();
        var s = c.prepareStatement(
            "insert into public.tasks (user_id, goal_id, title, category) values (?, ?, 'x', 'learn')")) {
      s.setObject(1, bob);
      s.setObject(2, aliceGoal);
      assertThrows(SQLException.class, s::executeUpdate);
    }
    assertEquals(0, taskCount(bob));
  }
}