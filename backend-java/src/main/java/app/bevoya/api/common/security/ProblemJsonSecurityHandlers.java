package app.bevoya.api.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 401 and 403 as RFC 7807 problem+json. Fixed text only: never echoes the token or the exception. */
final class ProblemJsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException ex) throws IOException {
    response.setHeader("WWW-Authenticate", "Bearer");
    write(response, 401, "Unauthorized", "Sign-in is required, or the access token is not valid.");
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException ex) throws IOException {
    write(response, 403, "Forbidden", "You do not have access to this resource.");
  }

  private static void write(HttpServletResponse response, int status, String title, String detail)
      throws IOException {
    StringBuilder body = new StringBuilder("{\"type\":\"about:blank\",\"title\":\"").append(title)
        .append("\",\"status\":").append(status)
        .append(",\"detail\":\"").append(detail).append('"');
    String requestId = MDC.get("requestId");
    if (requestId != null && requestId.matches("[A-Za-z0-9._-]{1,64}")) {
      body.append(",\"requestId\":\"").append(requestId).append('"');
    }
    body.append('}');
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response.setCharacterEncoding("UTF-8");
    response.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
  }
}