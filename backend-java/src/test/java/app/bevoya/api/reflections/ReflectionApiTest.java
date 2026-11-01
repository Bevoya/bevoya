package app.bevoya.api.reflections;

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

/** Reflections against a real database, as bevoya_app, with real tokens. Needs Docker (CI). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class ReflectionApiTest {

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

  private static long reflectionCount(UUID user) throws SQLException {
    try (Connection c = admin();
        var s = c.prepareStatement("select count(*) from public.reflections where user_id = ?")) {
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
    assertTrue(m.find(), "no id in response, status " + response.statusCode());
    return UUID.fromString(m.group(1));
  }

  private static String field(String body, String name) {
    Matcher m = Pattern.compile("\"" + name + "\":(null|true|false|\"[^\"]*\"|[0-9.\\-]+)").matcher(body);
    assertTrue(m.find(), "field " + name + " missing");
    return m.group(1);
  }

  private static String withoutPerRequestFields(String body) {
    return body.replaceAll("\"(requestId|instance)\":\"[^\"]*\",?", "");
  }

  private static void assertProblem(HttpResponse<String> response, int status) {
    assertEquals(status, response.statusCode());
    assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/problem+json"),
        response.headers().firstValue("content-type").orElse("none"));
  }

  private static String entry(String text) {
    return "{\"rawText\":\"" + text + "\"}";
  }

  private static String entryOn(String date, String text) {
    return "{\"entryDate\":\"" + date + "\",\"rawText\":\"" + text + "\"}";
  }

  private HttpResponse<String> post(UUID user, String json) throws Exception {
    return call("POST", "/reflections", tokenFor(user), json);
  }

  private UUID createOk(UUID user, String json) throws Exception {
    HttpResponse<String> created = post(user, json);
    assertEquals(201, created.statusCode());
    return idOf(created);
  }

  private List<UUID> idsOf(String path, UUID user) throws Exception {
    HttpResponse<String> response = call("GET", path, tokenFor(user), null);
    assertEquals(200, response.statusCode());
    List<UUID> ids = new ArrayList<>();
    Matcher m = ID.matcher(response.body());
    while (m.find()) {
      ids.add(UUID.fromString(m.group(1)));
    }
    return ids;
  }

  // ---- tests ----

  @Test
  void createReturnsTheEntryAndItIsReadableAgain() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = post(alice,
        "{\"entryDate\":\"2026-10-30\",\"rawText\":\"Built the search endpoint\",\"mood\":4,\"energy\":3}");
    assertEquals(201, created.statusCode());
    UUID id = idOf(created);
    assertEquals("/reflections/" + id, created.headers().firstValue("location").orElse(""));
    assertEquals("\"2026-10-30\"", field(created.body(), "entryDate"));
    assertEquals("\"Built the search endpoint\"", field(created.body(), "rawText"));
    assertEquals("4", field(created.body(), "mood"));
    assertEquals("3", field(created.body(), "energy"));

    HttpResponse<String> bare = post(alice, entry("Only text"));
    assertEquals(201, bare.statusCode());
    assertEquals("null", field(bare.body(), "mood"));
    assertEquals("null", field(bare.body(), "energy"));
    assertTrue(field(bare.body(), "entryDate").matches("\"\\d{4}-\\d{2}-\\d{2}\""), "a date is filled in");

    HttpResponse<String> read = call("GET", "/reflections/" + id, tokenFor(alice), null);
    assertEquals(200, read.statusCode());
    assertEquals(created.body(), read.body());
  }

  @Test
  void anEntryThatIsMissingAndAnEntryThatIsNotYoursGiveTheSame404() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID item = createOk(alice, entry("Alice private text"));
    UUID missing = UUID.randomUUID();
    String put = "{\"rawText\":\"Hijacked\"}";

    String[][] calls = {{"GET", null}, {"PUT", put}, {"DELETE", null}};
    for (String[] c : calls) {
      HttpResponse<String> foreign = call(c[0], "/reflections/" + item, tokenFor(bob), c[1]);
      HttpResponse<String> absent = call(c[0], "/reflections/" + missing, tokenFor(bob), c[1]);
      assertProblem(foreign, 404);
      assertProblem(absent, 404);
      assertEquals(withoutPerRequestFields(absent.body()), withoutPerRequestFields(foreign.body()), c[0]);
    }
    HttpResponse<String> mine = call("GET", "/reflections/" + item, tokenFor(alice), null);
    assertEquals(200, mine.statusCode());
    assertEquals("\"Alice private text\"", field(mine.body(), "rawText"));
  }

  @Test
  void listsOnlyYourOwnEntriesNewestDateFirstAndFiltersByFrom() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    UUID oldest = createOk(alice, entryOn("2026-10-01", "One"));
    UUID newest = createOk(alice, entryOn("2026-10-03", "Three"));
    UUID middle = createOk(alice, entryOn("2026-10-02", "Two"));
    createOk(bob, entryOn("2026-10-02", "Bob"));

    assertEquals(List.of(newest, middle, oldest), idsOf("/reflections", alice));
    assertEquals(List.of(newest, middle), idsOf("/reflections?from=2026-10-02", alice));
    assertEquals(1, idsOf("/reflections", bob).size());
    assertProblem(call("GET", "/reflections?from=yesterday", tokenFor(alice), null), 400);
  }

  @Test
  void putReplacesTextMoodAndEnergyAndKeepsTheDateWhenOmitted() throws Exception {
    UUID alice = signedUpUser();
    HttpResponse<String> created = post(alice,
        "{\"entryDate\":\"2026-10-20\",\"rawText\":\"Old\",\"mood\":2,\"energy\":2}");
    UUID id = idOf(created);

    HttpResponse<String> replaced = call("PUT", "/reflections/" + id, tokenFor(alice),
        "{\"rawText\":\"New text\"}");
    assertEquals(200, replaced.statusCode());
    assertEquals("\"New text\"", field(replaced.body(), "rawText"));
    assertEquals("\"2026-10-20\"", field(replaced.body(), "entryDate"), "an omitted date keeps the old one");
    assertEquals("null", field(replaced.body(), "mood"), "an omitted mood is cleared");
    assertEquals("null", field(replaced.body(), "energy"));
    assertEquals(field(created.body(), "createdAt"), field(replaced.body(), "createdAt"));
    assertNotEquals(field(created.body(), "updatedAt"), field(replaced.body(), "updatedAt"));

    HttpResponse<String> moved = call("PUT", "/reflections/" + id, tokenFor(alice),
        entryOn("2026-10-21", "Moved"));
    assertEquals("\"2026-10-21\"", field(moved.body(), "entryDate"));

    HttpResponse<String> reread = call("GET", "/reflections/" + id, tokenFor(alice), null);
    assertEquals(moved.body(), reread.body(), "the update must be persisted, not only returned");
  }

  @Test
  void deleteRemovesAnEntryOnce() throws Exception {
    UUID alice = signedUpUser();
    UUID id = createOk(alice, entry("Temporary"));
    assertEquals(204, call("DELETE", "/reflections/" + id, tokenFor(alice), null).statusCode());
    assertProblem(call("DELETE", "/reflections/" + id, tokenFor(alice), null), 404);
    assertProblem(call("GET", "/reflections/" + id, tokenFor(alice), null), 404);
    assertEquals(0, reflectionCount(alice));
  }

  @Test
  void invalidInputIsRejectedWithAProblemAndChangesNothing() throws Exception {
    UUID alice = signedUpUser();
    List<String> badBodies = List.of(
        "{}",
        "{\"rawText\":\"\"}",
        "{\"rawText\":\"   \"}",
        "{\"rawText\":\"" + "r".repeat(20001) + "\"}",
        "{\"rawText\":\"x\",\"mood\":0}",
        "{\"rawText\":\"x\",\"mood\":6}",
        "{\"rawText\":\"x\",\"energy\":0}",
        "{\"rawText\":\"x\",\"energy\":6}",
        "{\"rawText\":\"x\",\"entryDate\":\"30-10-2026\"}",
        "{\"rawText\":\"x\",\"entryDate\":\"2026-13-45\"}",
        "{not json");
    for (String body : badBodies) {
      assertProblem(post(alice, body), 400);
    }
    UUID id = createOk(alice, entry("Valid"));
    for (String body : badBodies) {
      assertProblem(call("PUT", "/reflections/" + id, tokenFor(alice), body), 400);
    }
    assertProblem(call("GET", "/reflections/not-a-uuid", tokenFor(alice), null), 400);
    assertEquals("\"Valid\"", field(call("GET", "/reflections/" + id, tokenFor(alice), null).body(), "rawText"));
    assertEquals(1, reflectionCount(alice));
  }

  @Test
  void missingTokenIsRejectedOnEveryRoute() throws Exception {
    String id = UUID.randomUUID().toString();
    String[][] routes = {
        {"POST", "/reflections", entry("x")},
        {"GET", "/reflections", null},
        {"GET", "/reflections/" + id, null},
        {"PUT", "/reflections/" + id, entry("x")},
        {"DELETE", "/reflections/" + id, null}};
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
    HttpResponse<String> first = callWithKey("POST", "/reflections", tokenFor(alice), entry("Original"), key);
    HttpResponse<String> retry = callWithKey("POST", "/reflections", tokenFor(alice), entry("Different"), key);
    assertEquals(201, first.statusCode());
    assertEquals(201, retry.statusCode());
    assertEquals(idOf(first), idOf(retry));
    assertEquals(first.body(), retry.body());
    assertEquals(1, reflectionCount(alice));
  }

  @Test
  void keysAreScopedPerUserMalformedKeysAreRefusedAndADeletedEntryReleasesItsKey() throws Exception {
    UUID alice = signedUpUser();
    UUID bob = signedUpUser();
    String key = UUID.randomUUID().toString();
    UUID aliceItem = idOf(callWithKey("POST", "/reflections", tokenFor(alice), entry("Alice"), key));
    UUID bobItem = idOf(callWithKey("POST", "/reflections", tokenFor(bob), entry("Bob"), key));
    assertNotEquals(aliceItem, bobItem);

    for (String bad : List.of("short", "has spaces in the key", "x".repeat(101))) {
      assertProblem(callWithKey("POST", "/reflections", tokenFor(alice), entry("Nope"), bad), 400);
    }
    assertEquals(1, reflectionCount(alice));

    assertEquals(204, call("DELETE", "/reflections/" + aliceItem, tokenFor(alice), null).statusCode());
    HttpResponse<String> again = callWithKey("POST", "/reflections", tokenFor(alice), entry("Alice"), key);
    assertEquals(201, again.statusCode());
    assertNotEquals(aliceItem, idOf(again));
    assertEquals(1, reflectionCount(alice));
  }
}