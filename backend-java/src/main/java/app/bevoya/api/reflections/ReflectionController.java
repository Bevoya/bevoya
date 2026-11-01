package app.bevoya.api.reflections;

import app.bevoya.api.common.idempotency.IdempotencyStore;
import app.bevoya.api.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reflections")
class ReflectionController {

  private final ReflectionService reflections;

  ReflectionController(ReflectionService reflections) {
    this.reflections = reflections;
  }

  @PostMapping
  ResponseEntity<ReflectionResponse> create(CurrentUser user,
      @RequestHeader(name = IdempotencyStore.HEADER, required = false)
      @Pattern(regexp = IdempotencyStore.KEY_PATTERN) String idempotencyKey,
      @Valid @RequestBody ReflectionRequest request) {
    ReflectionResponse created = reflections.create(user.authUserId(), idempotencyKey, request);
    return ResponseEntity.created(URI.create("/reflections/" + created.id())).body(created);
  }

  @GetMapping
  List<ReflectionResponse> list(CurrentUser user,
      @RequestParam(name = "from", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
    return reflections.list(user.authUserId(), from);
  }

  @GetMapping("/{id}")
  ReflectionResponse get(CurrentUser user, @PathVariable UUID id) {
    return reflections.get(user.authUserId(), id);
  }

  @PutMapping("/{id}")
  ReflectionResponse replace(CurrentUser user, @PathVariable UUID id,
      @Valid @RequestBody ReflectionRequest request) {
    return reflections.replace(user.authUserId(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(CurrentUser user, @PathVariable UUID id) {
    reflections.delete(user.authUserId(), id);
  }
}