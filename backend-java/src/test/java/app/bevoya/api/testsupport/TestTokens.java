package app.bevoya.api.testsupport;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Mints ES256 tokens for any subject and serves the matching JWKS on a local port. */
public final class TestTokens {

  // Must match src/test/resources/config/application.properties.
  public static final String ISSUER = "https://example.invalid/auth/v1";
  public static final String AUDIENCE = "authenticated";

  private static ECKey key;
  private static HttpServer jwks;

  private TestTokens() {
  }

  private static synchronized HttpServer server() {
    if (jwks == null) {
      try {
        key = new ECKeyGenerator(Curve.P_256).keyID("test-key").generate();
        byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/jwks.json", exchange -> {
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (var out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
        server.start();
        jwks = server;
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    return jwks;
  }

  public static String jwksUri() {
    return "http://127.0.0.1:" + server().getAddress().getPort() + "/jwks.json";
  }

  public static String tokenFor(UUID subject) {
    server();
    Instant now = Instant.now();
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(ISSUER).audience(AUDIENCE).subject(subject.toString())
        .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(600))).build();
    try {
      SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("test-key").build(), claims);
      jwt.sign(new ECDSASigner(key));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  public static synchronized void stop() {
    if (jwks != null) {
      jwks.stop(0);
      jwks = null;
    }
  }
}