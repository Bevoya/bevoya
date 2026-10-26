package app.bevoya.api.common.web;

import app.bevoya.api.common.security.UnknownAccountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 7807 style problem response
 * (application/problem+json) that carries the request ID.
 * Standard Spring MVC errors (404, 405, bad request and so on) are handled by
 * the parent class. Anything unexpected becomes a generic 500 that never
 * includes the exception message.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // Built with createResponseEntity on purpose: handleExceptionInternal would add an
    // "instance" with the request path, and then a missing goal and someone else's goal
    // would produce different bodies.
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "We couldn't find that.");
        problem.setTitle("Not found");
        return createResponseEntity(problem, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(UnknownAccountException.class)
    public ResponseEntity<Object> handleUnknownAccount(UnknownAccountException ex, WebRequest request) {
        log.warn("Valid token for an account that no longer exists");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Please sign in again.");
        problem.setTitle("Unauthorized");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return createResponseEntity(problem, headers, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        // Log the type only. Exception messages and stack traces can contain
        // fragments of request bodies, and user text must never reach the logs.
        log.error("Unhandled exception, type={}", ex.getClass().getName());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "We couldn't complete that. Try again in a moment.");
        problem.setTitle("Internal server error");
        return createResponseEntity(problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body,
                                                          HttpHeaders headers,
                                                          HttpStatusCode statusCode,
                                                          WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            String requestId = MDC.get(RequestIdFilter.MDC_KEY);
            if (requestId != null) {
                problem.setProperty("requestId", requestId);
            }
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }
}