package app.bevoya.api.goals;

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

/** Milestones against a real database, as bevoya_app, with real tokens. Needs Docker (CI). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class MilestoneApiTest {

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

  private static long milestoneCount(UUID goal) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("select count(*) from public.milestones where goal_id = ?")) {
      s.setObject(1, goal);
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
    assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/problem+json"));
  }

  private static String milestonesPath(UUID goal) {
    return "/goals/" + goal + "/milestones";
  }

  private UUID createGoal(UUID user, String title) throws Exception {
    HttpResponse<String> created = call("POST", "/goals", tokenFor(user), "{\"title\":\"" + title + "\"}");
    assertEquals(201, created.statusCode(), created.body());
    return idOf(created);
  }

  private HttpResponse<String> postMilestone(UUID user, UUID goal, String title) throws Exception {
    return call("POST", milestonesPath(goal), tokenFor(user), "{\"title\":\"" + title + "\"}");
  }

  private UUID createMilestone(UUID user, UUID goal, String title) throws Exception {
    HttpResponse<String> created = postMilestone(user, goal, title);
    assertEquals(201, created.statusCode(), created.body());
    return idOf(created);
  }

  private List<UUID> listIds(UUID user, UUID goal) throws Exception {
    HttpResponse<String> response = call("GET", milestonesPath(goal), tokenFor(user), null);
    assertEquals(200, response.statusCode(), response.body());
    List<UUID> ids = new ArrayList<>();
    Matcher m = ID.matcher(response.body());
    while (m.find()) {
      ids.add(UUID.fromString(m.group(1)));
    }
    return ids;
  }

  private static String orderJson(List<UUID> ids) {
    StringBuilder json = new StringBuilder("{\"milestoneIds\":[");
    for (int i = 0; i < ids.size(); i++) {
      json.append(i == 0 ? "" : ",").append('"').append(ids.get(i)).append('"');
    }
    return json.append("]}").toString();
  }

  // ---- tests ----

  @Test
  void milestonesAreAppendedInOrderAndListedBack() throws Exception {
    UUID alice = signedUpUser();
    UUID goal = createGoal(alice, "Goal");
    assertEquals("[]", call("GET", milestonesPath(goal), tokenFor(alice), null).body());

    HttpResponse<String> first = postMilestone(alice, goal, "First");
    assertEquals(201, first.statusCode(), first.body());
    UUID m1 = idOf(first);
    assertEquals("/goals/" + goal + "/milestones/" + m1, first.headers().firstValue("location").orElse(""));
    assertEquals("\"not_started\"", field(first.body(), "status"));
    assertEquals("0", field(first.body(), "position"));
    HttpResponse<String> second = postMilestone(alice, goal, "Second");
    assertEquals("1", field(second.body(), "position"));
    UUID m2 = idOf(second);
    UUID m3 = createMilestone(alice, goal, "Third");

    assertEquals(List.of(m1, m2, m3), listIds(alice, goal));
  }

  @Test
  void positionsKeepGrowingAfterADelete() throws Exception {
    UUID alice = signedUpUser();
    UUID goal = createGoal(alice, "Goal");
    UUID a = createMilestone(alice, goal, "A");
    UUID b = createMilestone(alice, goal, "B");
    UUID c = createMilestone(alice, goal, "C");
    assertEquals(204, call("DELETE", milestonesPath(goal) + "/" + b, tokenFor(alice), null).statusCode());
    HttpResponse<String> d = postMilestone(alice, goal, "D");
    assertEquals("3", field(d.body(), "position"));
    assertEquals(List.of(a, c, idOf(d)), listIds(alice, goal));
  }

  @Test
  void aGoalThatIsMissingAndAGoalThatIsNotYoursGiveTheSame404() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID goal = createGoal(alice, "Alice's goal");
    UUID milestone = createMilestone(alice, goal, "Alice's milestone");
    UUID missing = UUID.randomUUID();
    String replace = "{\"title\":\"Hijacked\",\"status\":\"completed\"}";

    String[][] calls = {
        {"POST", "", "{\"title\":\"Sneaky\"}"},
        {"GET", "", null},
        {"PUT", "/" + milestone, replace},
        {"DELETE", "/" + milestone, null},
        {"PUT", "/order", "{\"milestoneIds\":[]}"}};
    for (String[] c : calls) {
      HttpResponse<String> foreign = call(c[0], milestonesPath(goal) + c[1], tokenFor(bob), c[2]);
      HttpResponse<String> absent = call(c[0], milestonesPath(missing) + c[1], tokenFor(bob), c[2]);
      assertProblem(foreign, 404);
      assertProblem(absent, 404);
      assertEquals(withoutPerRequestFields(absent.body()), withoutPerRequestFields(foreign.body()), c[0] + c[1]);
    }

    assertEquals(List.of(milestone), listIds(alice, goal));
    assertTrue(call("GET", milestonesPath(goal), tokenFor(alice), null).body()
        .contains("\"title\":\"Alice's milestone\""));
  }

  @Test
  void aMilestoneIsOnlyReachableThroughItsOwnGoal() throws Exception {
    UUID alice = signedUpUser();
    UUID goalA = createGoal(alice, "A");
    UUID goalB = createGoal(alice, "B");
    UUID milestone = createMilestone(alice, goalA, "In A");

    String replace = "{\"title\":\"Moved?\",\"status\":\"completed\"}";
    assertProblem(call("PUT", milestonesPath(goalB) + "/" + milestone, tokenFor(alice), replace), 404);
    assertProblem(call("DELETE", milestonesPath(goalB) + "/" + milestone, tokenFor(alice), null), 404);
    assertEquals(List.of(milestone), listIds(alice, goalA));
    assertEquals(List.of(), listIds(alice, goalB));
  }

  @Test
  void putReplacesTheContentAndKeepsThePosition() throws Exception {
    UUID alice = signedUpUser();
    UUID goal = createGoal(alice, "Goal");
    HttpResponse<String> created = postMilestone(alice, goal, "Draft");
    UUID m1 = idOf(created);
    createMilestone(alice, goal, "Other");

    HttpResponse<String> replaced = call("PUT", milestonesPath(goal) + "/" + m1, tokenFor(alice),
        "{\"title\":\"  Final  \",\"description\":\"Why it matters\",\"status\":\"in_progress\","
            + "\"targetDate\":\"2027-02-01\"}");
    assertEquals(200, replaced.statusCode(), replaced.body());
    assertEquals("\"Final\"", field(replaced.body(), "title"));
    assertEquals("\"in_progress\"", field(replaced.body(), "status"));
    assertEquals("\"2027-02-01\"", field(replaced.body(), "targetDate"));
    assertEquals("0", field(replaced.body(), "position"));
    assertEquals(field(created.body(), "createdAt"), field(replaced.body(), "createdAt"));
    assertNotEquals(field(created.body(), "updatedAt"), field(replaced.body(), "updatedAt"));

    String listed = call("GET", milestonesPath(goal), tokenFor(alice), null).body();
    assertTrue(listed.contains("Why it matters"), "the update must be persisted, not only returned");
  }

  @Test
  void reorderChangesTheListOrderAndItPersists() throws Exception {
    UUID alice = signedUpUser();
    UUID goal = createGoal(alice, "Goal");
    UUID m1 = createMilestone(alice, goal, "One");
    UUID m2 = createMilestone(alice, goal, "Two");
    UUID m3 = createMilestone(alice, goal, "Three");

    HttpResponse<String> reordered = call("PUT", milestonesPath(goal) + "/order", tokenFor(alice),
        orderJson(List.of(m3, m1, m2)));
    assertEquals(200, reordered.statusCode(), reordered.body());
    Matcher m = ID.matcher(reordered.body());
    List<UUID> returned = new ArrayList<>();
    while (m.find()) {
      returned.add(UUID.fromString(m.group(1)));
    }
    assertEquals(List.of(m3, m1, m2), returned);
    assertEquals(List.of(m3, m1, m2), listIds(alice, goal));
  }

  @Test
  void reorderRejectsAnythingButTheExactSetAndChangesNothing() throws Exception {
    UUID alice = signedUpUser();
    UUID goal = createGoal(alice, "Goal");
    UUID other = createGoal(alice, "Other goal");
    UUID m1 = createMilestone(alice, goal, "One");
    UUID m2 = createMilestone(alice, goal, "Two");
    UUID m3 = createMilestone(alice, goal, "Three");
    UUID elsewhere = createMilestone(alice, other, "Elsewhere");

    List<String> bad = List.of(
        orderJson(List.of(m1, m2)),
        orderJson(List.of(m1, m2, m3, UUID.randomUUID())),
        orderJson(List.of(m1, m1, m2)),
        orderJson(List.of(m1, m2, m2)),
        orderJson(List.of(m1, m2, elsewhere)),
        orderJson(List.of()),
        "{\"milestoneIds\":null}",
        "{\"milestoneIds\":[\"not-a-uuid\"]}",
        "{}");
    for (String body : bad) {
      assertProblem(call("PUT", milestonesPath(goal) + "/order", tokenFor(alice), body), 400);
    }
    assertEquals(List.of(m1, m2, m3), listIds(alice, goal));
    assertEquals(List.of(elsewhere), listIds(alice, other));
  }

  @Test
  void deleteRemovesAMilestoneOnceAndDeletingTheGoalRemovesItsMilestones() throws Exception {
    UUID alice = signedUpUser();
    UUID goal = createGoal(alice, "Goal");
    UUID m1 = createMilestone(alice, goal, "One");
    createMilestone(alice, goal, "Two");

    String path = milestonesPath(goal) + "/" + m1;
    assertEquals(204, call("DELETE", path, tokenFor(alice), null).statusCode());
    assertProblem(call("DELETE", path, tokenFor(alice), null), 404);
    assertEquals(1, milestoneCount(goal));

    assertEquals(204, call("DELETE", "/goals/" + goal, tokenFor(alice), null).statusCode());
    assertEquals(0, milestoneCount(goal));
  }

  @Test
  void invalidInputIsRejectedWithAProblemAndCreatesNothing() throws Exception {
    UUID alice = signedUpUser();
    String token = tokenFor(alice);
    UUID goal = createGoal(alice, "Goal");
    List<String> badBodies = List.of(
        "{}",
        "{\"title\":\"\"}",
        "{\"title\":\"   \"}",
        "{\"title\":\"" + "x".repeat(201) + "\"}",
        "{\"title\":\"ok\",\"status\":\"nope\"}",
        "{\"title\":\"ok\",\"description\":\"" + "d".repeat(2001) + "\"}",
        "{\"title\":\"ok\",\"targetDate\":\"next week\"}",
        "{not json");
    for (String body : badBodies) {
      assertProblem(call("POST", milestonesPath(goal), token, body), 400);
    }
    UUID m1 = createMilestone(alice, goal, "Valid");
    assertProblem(call("PUT", milestonesPath(goal) + "/" + m1, token, "{\"title\":\"No status\"}"), 400);
    assertProblem(call("GET", "/goals/not-a-uuid/milestones", token, null), 400);
    assertEquals(1, milestoneCount(goal));
  }

  @Test
  void missingTokenIsRejectedOnEveryRoute() throws Exception {
    String goal = UUID.randomUUID().toString();
    String id = UUID.randomUUID().toString();
    String json = "{\"title\":\"x\",\"status\":\"not_started\"}";
    String[][] routes = {
        {"POST", "/goals/" + goal + "/milestones", json},
        {"GET", "/goals/" + goal + "/milestones", null},
        {"PUT", "/goals/" + goal + "/milestones/" + id, json},
        {"DELETE", "/goals/" + goal + "/milestones/" + id, null},
        {"PUT", "/goals/" + goal + "/milestones/order", "{\"milestoneIds\":[]}"}};
    for (String[] route : routes) {
      HttpResponse<String> response = call(route[0], route[1], null, route[2]);
      assertProblem(response, 401);
      assertTrue(response.headers().firstValue("www-authenticate").orElse("").startsWith("Bearer"));
    }
  }

  @Test
  void sameKeyTwiceCreatesOneMilestoneAndReturnsTheOriginal() throws Exception {
    UUID alice = signedUpUser();
    UUID goal = createGoal(alice, "Goal");
    String key = UUID.randomUUID().toString();
    HttpResponse<String> first = callWithKey("POST", milestonesPath(goal), tokenFor(alice),
        "{\"title\":\"Original\"}", key);
    HttpResponse<String> retry = callWithKey("POST", milestonesPath(goal), tokenFor(alice),
        "{\"title\":\"A different title\"}", key);
    assertEquals(201, first.statusCode(), first.body());
    assertEquals(201, retry.statusCode(), retry.body());
    assertEquals(idOf(first), idOf(retry));
    assertEquals(first.body(), retry.body(), "a retry returns the original milestone unchanged");
    assertEquals(1, milestoneCount(goal));
  }

  @Test
  void aKeyUsedForAnotherGoalIsRefused() throws Exception {
    UUID alice = signedUpUser();
    UUID goalA = createGoal(alice, "A");
    UUID goalB = createGoal(alice, "B");
    String key = UUID.randomUUID().toString();
    assertEquals(201, callWithKey("POST", milestonesPath(goalA), tokenFor(alice),
        "{\"title\":\"In A\"}", key).statusCode());
    HttpResponse<String> reused = callWithKey("POST", milestonesPath(goalB), tokenFor(alice),
        "{\"title\":\"In B\"}", key);
    assertProblem(reused, 409);
    assertEquals(0, milestoneCount(goalB));
  }

  @Test
  void malformedKeyIsRejectedAndRetryAfterDeleteCreatesAFreshOne() throws Exception {
    UUID alice = signedUpUser();
    UUID goal = createGoal(alice, "Goal");
    for (String key : List.of("short", "has spaces in the key", "x".repeat(101))) {
      assertProblem(callWithKey("POST", milestonesPath(goal), tokenFor(alice), "{\"title\":\"Nope\"}", key), 400);
    }
    assertEquals(0, milestoneCount(goal));

    String key = UUID.randomUUID().toString();
    UUID first = idOf(callWithKey("POST", milestonesPath(goal), tokenFor(alice), "{\"title\":\"Once\"}", key));
    assertEquals(204, call("DELETE", milestonesPath(goal) + "/" + first, tokenFor(alice), null).statusCode());
    HttpResponse<String> again = callWithKey("POST", milestonesPath(goal), tokenFor(alice),
        "{\"title\":\"Once\"}", key);
    assertEquals(201, again.statusCode(), again.body());
    assertNotEquals(first, idOf(again));
    assertEquals(1, milestoneCount(goal));
  }

  @Test
  void theDatabaseRefusesAMilestoneThatPointsAtAnotherUsersGoal() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID aliceGoal = createGoal(alice, "Alice's goal");
    assertEquals(200, call("GET", "/me", tokenFor(bob), null).statusCode()); // creates Bob's profile
    try (Connection c = admin();
        var s = c.prepareStatement(
            "insert into public.milestones (user_id, goal_id, title, position) values (?, ?, 'x', 0)")) {
      s.setObject(1, bob);
      s.setObject(2, aliceGoal);
      assertThrows(SQLException.class, s::executeUpdate);
    }
    assertEquals(0, milestoneCount(aliceGoal));
  }
}