package app.bevoya.api.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bevoya.api.identity.ProfileService;
import app.bevoya.api.profiles.ProfileDetailsService;
import app.bevoya.api.profiles.ProfileResponse;
import app.bevoya.api.profiles.UpdateProfileRequest;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthTokenTest {

  // Must match src/test/resources/config/application.properties.
  private static final String ISSUER = "https://example.invalid/auth/v1";
  private static final String AUDIENCE = "authenticated";
  private static final String SUBJECT = "11111111-2222-3333-4444-555555555555";

  private static final ECKey KEY;
  private static final HttpServer JWKS;

  /** Ids the stubs were asked to create or read. No database is involved in this test. */
  private static final List<UUID> ENSURED = new CopyOnWriteArrayList<>();

  @TestConfiguration
  static class NoDatabaseProfiles {
    @Bean
    @Primary
    ProfileService recordingProfileService() {
      return id -> ENSURED.add(id);
    }

    /**
     * GET /me reads the profile. The real service needs a database, so answer from memory.
     * The real service ensures the profile exists before it reads, so the stub records that too.
     * (ProfileApiTest proves the real behaviour against a real database in CI.)
     */
    @Bean
    @Primary
    ProfileDetailsService inMemoryProfileDetails() {
      return new ProfileDetailsService() {
        @Override
        public ProfileResponse get(UUID userId) {
          ENSURED.add(userId);
          Instant now = Instant.now();
          return new ProfileResponse(userId, null, "other", "UTC", null, now, now);
        }

        @Override
        public ProfileResponse update(UUID userId, UpdateProfileRequest request) {
          throw new UnsupportedOperationException("not used by the token tests");
        }
      };
    }
  }

  static {
    try {
      KEY = new ECKeyGenerator(Curve.P_256).keyID("test-key").generate();
      byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
      JWKS = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
      JWKS.createContext("/jwks.json", exchange -> {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
          out.write(body);
        }
      });
      JWKS.start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void jwks(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/jwks.json");
  }

  @AfterAll
  static void stopJwks() {
    JWKS.stop(0);
  }

  @Value("${local.server.port}")
  int port;

  private final HttpClient client = HttpClient.newHttpClient();

  private HttpResponse<String> get(String path, String token) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static JWTClaimsSet.Builder claims() {
    Instant now = Instant.now();
    return new JWTClaimsSet.Builder().issuer(ISSUER).audience(AUDIENCE).subject(SUBJECT)
        .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(600)));
  }

  private static String signEs256(JWTClaimsSet claims, ECKey key) throws JOSEException {
    SignedJWT jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("test-key").build(), claims);
    jwt.sign(new ECDSASigner(key));
    return jwt.serialize();
  }

  private void assertRejected(String token) throws Exception {
    HttpResponse<String> response = get("/me", token);
    assertEquals(401, response.statusCode());
    assertTrue(response.headers().firstValue("www-authenticate").orElse("").startsWith("Bearer"));
    assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/problem+json"));
  }

  @Test
  void validTokenReturnsTheSubject() throws Exception {
    HttpResponse<String> response = get("/me", signEs256(claims().build(), KEY));
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains(SUBJECT));
  }

  @Test
  void meEnsuresTheProfileExistsForTheSubject() throws Exception {
    ENSURED.clear();
    assertEquals(200, get("/me", signEs256(claims().build(), KEY)).statusCode());
    assertEquals(List.of(UUID.fromString(SUBJECT)), ENSURED);
  }

  @Test
  void rejectedTokenNeverCreatesAProfile() throws Exception {
    ENSURED.clear();
    ECKey other = new ECKeyGenerator(Curve.P_256).keyID("test-key").generate();
    assertRejected(signEs256(claims().build(), other));
    assertTrue(ENSURED.isEmpty(), "no profile may be created for a request that failed authentication");
  }

  @Test
  void healthStaysPublic() throws Exception {
    assertEquals(200, get("/health", null).statusCode());
  }

  @Test
  void missingTokenIsRejected() throws Exception {
    assertRejected(null);
  }

  @Test
  void unauthorizedResponsesCarryARequestId() throws Exception {
    HttpResponse<String> response = get("/me", null);
    assertTrue(response.headers().firstValue("x-request-id").isPresent(),
        "401 responses should carry x-request-id");
  }

  @Test
  void expiredTokenIsRejected() throws Exception {
    Instant past = Instant.now().minusSeconds(600);
    assertRejected(signEs256(claims().issueTime(Date.from(past.minusSeconds(60)))
        .expirationTime(Date.from(past)).build(), KEY));
  }

  @Test
  void wrongIssuerIsRejected() throws Exception {
    assertRejected(signEs256(claims().issuer("https://evil.invalid/auth/v1").build(), KEY));
  }

  @Test
  void wrongAudienceIsRejected() throws Exception {
    assertRejected(signEs256(claims().audience("anon").build(), KEY));
  }

  @Test
  void unsignedTokenIsRejected() throws Exception {
    assertRejected(new PlainJWT(claims().build()).serialize());
  }

  @Test
  void wrongAlgorithmIsRejected() throws Exception {
    byte[] secret = new byte[32];
    new SecureRandom().nextBytes(secret);
    SignedJWT jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("test-key").build(), claims().build());
    jwt.sign(new MACSigner(secret));
    assertRejected(jwt.serialize());
  }

  @Test
  void tokenSignedByAnotherKeyIsRejected() throws Exception {
    ECKey other = new ECKeyGenerator(Curve.P_256).keyID("test-key").generate();
    assertRejected(signEs256(claims().build(), other));
  }
}