package app.bevoya.api.sessions;

import app.bevoya.api.common.idempotency.IdempotencyStore;
import app.bevoya.api.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
class SessionController {

  private final SessionService sessions;

  SessionController(SessionService sessions) {
    this.sessions = sessions;
  }

  @PostMapping
  ResponseEntity<SessionResponse> start(CurrentUser user,
      @RequestHeader(name = IdempotencyStore.HEADER, required = false)
      @Pattern(regexp = IdempotencyStore.KEY_PATTERN) String idempotencyKey,
      @Valid @RequestBody StartSessionRequest request) {
    SessionResponse started = sessions.start(user.authUserId(), idempotencyKey, request);
    return ResponseEntity.created(URI.create("/sessions/" + started.id())).body(started);
  }

  @PostMapping("/{id}/complete")
  SessionResponse complete(CurrentUser user, @PathVariable UUID id,
      @Valid @RequestBody(required = false) CompleteSessionRequest request) {
    return sessions.complete(user.authUserId(), id, request);
  }

  /** The running session, or 204: lets the app restore its timer from startedAt. */
  @GetMapping("/active")
  ResponseEntity<SessionResponse> active(CurrentUser user) {
    Optional<SessionResponse> running = sessions.active(user.authUserId());
    return running.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
  }

  @GetMapping
  List<SessionResponse> list(CurrentUser user,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
    return sessions.list(user.authUserId(), from);
  }

  @GetMapping("/{id}")
  SessionResponse get(CurrentUser user, @PathVariable UUID id) {
    return sessions.get(user.authUserId(), id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(CurrentUser user, @PathVariable UUID id) {
    sessions.delete(user.authUserId(), id);
  }
}