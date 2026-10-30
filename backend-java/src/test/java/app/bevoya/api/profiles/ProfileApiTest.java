package app.bevoya.api.profiles;

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

/** Profile, consent and AI-settings endpoints against a real database. Needs Docker (CI). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class ProfileApiTest {

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

  private static long count(String sql, UUID id) throws SQLException {
    try (Connection c = admin(); var s = c.prepareStatement(sql)) {
      s.setObject(1, id);
      try (ResultSet rs = s.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private static long consentRows(UUID user) throws SQLException {
    return count("select count(*) from public.consent_records where user_id = ?", user);
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

  private static String field(String body, String name) {
    Matcher m = Pattern.compile("\"" + name + "\":(null|true|false|\"[^\"]*\"|[0-9.\\-]+)").matcher(body);
    assertTrue(m.find(), body);
    return m.group(1);
  }

  private static UUID idOf(HttpResponse<String> response) {
    Matcher m = ID.matcher(response.body());
    assertTrue(m.find(), response.body());
    return UUID.fromString(m.group(1));
  }

  private static void assertProblem(HttpResponse<String> response, int status) {
    assertEquals(status, response.statusCode(), response.body());
    assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/problem+json"));
  }

  private static String consentJson(String type, boolean granted) {
    return "{\"consentType\":\"" + type + "\",\"granted\":" + granted + ",\"policyVersion\":\"2026-10\"}";
  }

  private static String aiJson(boolean memory, boolean journal) {
    return "{\"aiMemoryEnabled\":" + memory + ",\"journalRetrievalEnabled\":" + journal
        + ",\"policyVersion\":\"2026-10\"}";
  }

  // ---- profile ----

  @Test
  void firstMeCallCreatesTheProfileWithDefaults() throws Exception {
    UUID alice = signedUpUser();
    String profileSql = "select count(*) from public.profiles where id = ?";
    assertEquals(0, count(profileSql, alice));
    HttpResponse<String> me = call("GET", "/me", tokenFor(alice), null);
    assertEquals(200, me.statusCode(), me.body());
    assertTrue(me.body().contains("\"authUserId\":\"" + alice + "\""), me.body());
    assertEquals("\"other\"", field(me.body(), "userType"));
    assertEquals("\"UTC\"", field(me.body(), "timeZone"));
    assertEquals("null", field(me.body(), "displayName"));
    assertEquals(1, count(profileSql, alice));
  }

  @Test
  void patchChangesOnlyTheFieldsSentAndPersists() throws Exception {
    UUID alice = signedUpUser();
    String token = tokenFor(alice);
    HttpResponse<String> first = call("PATCH", "/me", token,
        "{\"displayName\":\"  Alice  \",\"userType\":\"student\",\"timeZone\":\"Asia/Kolkata\","
            + "\"availableMinutesPerDay\":90}");
    assertEquals(200, first.statusCode(), first.body());
    assertEquals("\"Alice\"", field(first.body(), "displayName"));
    assertEquals("\"student\"", field(first.body(), "userType"));
    assertEquals("\"Asia/Kolkata\"", field(first.body(), "timeZone"));
    assertEquals("90", field(first.body(), "availableMinutesPerDay"));

    HttpResponse<String> second = call("PATCH", "/me", token, "{\"availableMinutesPerDay\":30}");
    assertEquals(200, second.statusCode(), second.body());
    assertEquals("30", field(second.body(), "availableMinutesPerDay"));
    assertEquals("\"Alice\"", field(second.body(), "displayName"));
    assertEquals(field(first.body(), "createdAt"), field(second.body(), "createdAt"));
    assertNotEquals(field(first.body(), "updatedAt"), field(second.body(), "updatedAt"));

    HttpResponse<String> fetched = call("GET", "/me", token, null);
    assertEquals("\"Asia/Kolkata\"", field(fetched.body(), "timeZone"));
    assertEquals("30", field(fetched.body(), "availableMinutesPerDay"));
  }

  @Test
  void patchRejectsBadInputWithAProblem() throws Exception {
    UUID alice = signedUpUser();
    List<String> bad = List.of(
        "{\"userType\":\"astronaut\"}",
        "{\"timeZone\":\"Mars/Base\"}",
        "{\"availableMinutesPerDay\":1441}",
        "{\"availableMinutesPerDay\":-1}",
        "{\"displayName\":\"   \"}",
        "{\"displayName\":\"" + "n".repeat(81) + "\"}",
        "{not json");
    for (String body : bad) {
      assertProblem(call("PATCH", "/me", tokenFor(alice), body), 400);
    }
  }

  @Test
  void profilesAreIndependentBetweenUsers() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    assertEquals(200, call("PATCH", "/me", tokenFor(alice), "{\"displayName\":\"Alice\"}").statusCode());
    HttpResponse<String> bobMe = call("GET", "/me", tokenFor(bob), null);
    assertEquals("null", field(bobMe.body(), "displayName"));
    assertTrue(bobMe.body().contains(bob.toString()));
  }

  @Test
  void tokenOfADeletedAccountIsRefusedOnPatch() throws Exception {
    UUID ghost = UUID.randomUUID();
    HttpResponse<String> response = call("PATCH", "/me", tokenFor(ghost), "{\"displayName\":\"Ghost\"}");
    assertProblem(response, 401);
  }

  @Test
  void missingTokenIsRejectedOnEveryRoute() throws Exception {
    String[][] routes = {
        {"GET", "/me"}, {"PATCH", "/me"}, {"GET", "/me/consents"}, {"POST", "/me/consents"},
        {"GET", "/me/ai-settings"}, {"PUT", "/me/ai-settings"}};
    for (String[] route : routes) {
      boolean hasBody = route[0].equals("PATCH") || route[0].equals("POST") || route[0].equals("PUT");
      HttpResponse<String> response = call(route[0], route[1], null, hasBody ? "{}" : null);
      assertProblem(response, 401);
    }
  }

  // ---- consents ----

  @Test
  void consentsAreAppendOnlyAndTheLatestRowWins() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    String token = tokenFor(alice);
    assertEquals("[]", call("GET", "/me/consents", token, null).body());

    assertEquals(201, call("POST", "/me/consents", token, consentJson("terms", true)).statusCode());
    assertEquals(201, call("POST", "/me/consents", token, consentJson("terms", false)).statusCode());

    String current = call("GET", "/me/consents", token, null).body();
    assertEquals("false", field(current, "granted"));
    assertEquals(1, current.split("\"consentType\"", -1).length - 1, "one row per type");
    assertEquals(2, consentRows(alice), "withdrawing keeps the history");
    assertEquals("[]", call("GET", "/me/consents", tokenFor(bob), null).body());
  }

  @Test
  void theAppRoleCannotUpdateOrDeleteConsentRows() throws Exception {
    UUID alice = signedUpUser();
    assertEquals(201, call("POST", "/me/consents", tokenFor(alice), consentJson("privacy_policy", true))
        .statusCode());
    try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "bevoya_app", APP_PASSWORD);
        Statement s = c.createStatement()) {
      assertThrows(SQLException.class, () -> s.executeUpdate("update public.consent_records set granted = false"));
      assertThrows(SQLException.class, () -> s.executeUpdate("delete from public.consent_records"));
    }
    assertEquals(1, consentRows(alice));
  }

  @Test
  void invalidConsentBodiesAreRejected() throws Exception {
    UUID alice = signedUpUser();
    List<String> bad = List.of(
        "{}",
        "{\"consentType\":\"marketing\",\"granted\":true,\"policyVersion\":\"v1\"}",
        "{\"consentType\":\"terms\",\"policyVersion\":\"v1\"}",
        "{\"consentType\":\"terms\",\"granted\":true,\"policyVersion\":\"\"}",
        "{\"consentType\":\"terms\",\"granted\":true,\"policyVersion\":\"has spaces\"}",
        "{not json");
    for (String body : bad) {
      assertProblem(call("POST", "/me/consents", tokenFor(alice), body), 400);
    }
    assertEquals(0, consentRows(alice));
  }

  @Test
  void aReplayedConsentWithTheSameKeyReturnsTheOriginalAndNeverFlipsTheState() throws Exception {
    UUID alice = signedUpUser();
    String token = tokenFor(alice);
    String key = UUID.randomUUID().toString();

    HttpResponse<String> grant = callWithKey("POST", "/me/consents", token, consentJson("ai_memory", true), key);
    assertEquals(201, grant.statusCode(), grant.body());
    assertEquals(201, call("POST", "/me/consents", token, consentJson("ai_memory", false)).statusCode());

    // The slow first request is retried after the withdrawal was already recorded.
    HttpResponse<String> replay = callWithKey("POST", "/me/consents", token, consentJson("ai_memory", true), key);
    assertEquals(201, replay.statusCode(), replay.body());
    assertEquals(idOf(grant), idOf(replay));
    assertEquals(2, consentRows(alice), "the replay must not add a row");
    assertEquals("false", field(call("GET", "/me/consents", token, null).body(), "granted"));
  }

  // ---- AI settings ----

  @Test
  void aiSettingsDefaultOffAndWriteOnlyWhatChanges() throws Exception {
    UUID alice = signedUpUser();
    String token = tokenFor(alice);
    HttpResponse<String> start = call("GET", "/me/ai-settings", token, null);
    assertEquals(200, start.statusCode(), start.body());
    assertEquals("false", field(start.body(), "aiMemoryEnabled"));
    assertEquals("false", field(start.body(), "journalRetrievalEnabled"));

    assertEquals(200, call("PUT", "/me/ai-settings", token, aiJson(true, false)).statusCode());
    assertEquals(1, consentRows(alice));
    assertEquals(200, call("PUT", "/me/ai-settings", token, aiJson(true, false)).statusCode());
    assertEquals(1, consentRows(alice), "an unchanged setting writes nothing");

    assertEquals(200, call("PUT", "/me/ai-settings", token, aiJson(true, true)).statusCode());
    assertEquals(2, consentRows(alice));
    HttpResponse<String> both = call("GET", "/me/ai-settings", token, null);
    assertEquals("true", field(both.body(), "aiMemoryEnabled"));
    assertEquals("true", field(both.body(), "journalRetrievalEnabled"));

    assertEquals(200, call("PUT", "/me/ai-settings", token, aiJson(false, false)).statusCode());
    assertEquals(4, consentRows(alice));
    HttpResponse<String> off = call("GET", "/me/ai-settings", token, null);
    assertEquals("false", field(off.body(), "aiMemoryEnabled"));
    assertEquals("false", field(off.body(), "journalRetrievalEnabled"));
  }

  @Test
  void journalRetrievalCannotBeOnWithoutAiMemory() throws Exception {
    UUID alice = signedUpUser();
    assertProblem(call("PUT", "/me/ai-settings", tokenFor(alice), aiJson(false, true)), 400);
    assertProblem(call("PUT", "/me/ai-settings", tokenFor(alice),
        "{\"aiMemoryEnabled\":true,\"policyVersion\":\"2026-10\"}"), 400);
    assertEquals(0, consentRows(alice));
  }

  @Test
  void aiSettingsAreIndependentBetweenUsers() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    assertEquals(200, call("PUT", "/me/ai-settings", tokenFor(alice), aiJson(true, true)).statusCode());
    HttpResponse<String> bobSettings = call("GET", "/me/ai-settings", tokenFor(bob), null);
    assertEquals("false", field(bobSettings.body(), "aiMemoryEnabled"));
    assertEquals("false", field(bobSettings.body(), "journalRetrievalEnabled"));
  }
}