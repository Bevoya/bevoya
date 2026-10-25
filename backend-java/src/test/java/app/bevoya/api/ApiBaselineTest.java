package app.bevoya.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bevoya.api.common.security.PermitAllSecurityTestConfig;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PermitAllSecurityTestConfig.class)
class ApiBaselineTest {

    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> send(String method, String path, String requestId) throws Exception {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (requestId != null) {
            builder.header("X-Request-Id", requestId);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String header(HttpResponse<String> response, String name) {
        return response.headers().firstValue(name).orElse("");
    }

    @Test
    void healthReturnsOkWithRequestId() throws Exception {
        HttpResponse<String> response = send("GET", "/health", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"ok\""), response.body());
        assertTrue(header(response, "X-Request-Id").matches(UUID_PATTERN),
                "a UUID request id is generated when none is sent");
    }

    @Test
    void safeRequestIdFromCallerIsEchoed() throws Exception {
        HttpResponse<String> response = send("GET", "/health", "client-req-12345");

        assertEquals("client-req-12345", header(response, "X-Request-Id"));
    }

    @Test
    void unsafeRequestIdIsReplaced() throws Exception {
        HttpResponse<String> response = send("GET", "/health", "bad id with spaces!");

        String id = header(response, "X-Request-Id");
        assertNotEquals("bad id with spaces!", id);
        assertTrue(id.matches(UUID_PATTERN), id);
    }

    @Test
    void unknownPathWithoutTokenIsUnauthorizedProblemJson() throws Exception {
        // Unauthenticated callers cannot tell which routes exist: unknown paths answer 401, not 404.
        HttpResponse<String> response = send("GET", "/no-such-path", null);

        assertEquals(401, response.statusCode());
        assertTrue(header(response, "Content-Type").startsWith("application/problem+json"),
                header(response, "Content-Type"));
        assertTrue(response.body().contains("\"status\":401"), response.body());
        assertTrue(response.body().contains("\"requestId\""), response.body());
    }

    @Test
    void wrongMethodReturnsProblemJson() throws Exception {
        HttpResponse<String> response = send("POST", "/health", null);

        assertEquals(405, response.statusCode());
        assertTrue(header(response, "Content-Type").startsWith("application/problem+json"),
                header(response, "Content-Type"));
    }

    @Test
    void unexpectedErrorIsGenericAndLeaksNothing() throws Exception {
        HttpResponse<String> response = send("GET", "/test-only/boom", null);

        assertEquals(500, response.statusCode());
        assertTrue(header(response, "Content-Type").startsWith("application/problem+json"),
                header(response, "Content-Type"));
        assertTrue(response.body().contains("\"status\":500"), response.body());
        assertTrue(response.body().contains("\"requestId\""), response.body());
        assertFalse(response.body().contains("private text"), response.body());
        assertFalse(response.body().contains("IllegalStateException"), response.body());
    }
}
