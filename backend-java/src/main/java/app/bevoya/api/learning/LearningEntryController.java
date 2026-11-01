package app.bevoya.api.learning;

import app.bevoya.api.common.idempotency.IdempotencyStore;
import app.bevoya.api.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/learning-entries")
class LearningEntryController {

  private final LearningEntryService entries;

  LearningEntryController(LearningEntryService entries) {
    this.entries = entries;
  }

  @PostMapping
  ResponseEntity<LearningEntryResponse> create(CurrentUser user,
      @RequestHeader(name = IdempotencyStore.HEADER, required = false)
      @Pattern(regexp = IdempotencyStore.KEY_PATTERN) String idempotencyKey,
      @Valid @RequestBody LearningEntryRequest request) {
    LearningEntryResponse created = entries.create(user.authUserId(), idempotencyKey, request);
    return ResponseEntity.created(URI.create("/learning-entries/" + created.id())).body(created);
  }

  @GetMapping
  List<LearningEntryResponse> list(CurrentUser user) {
    return entries.list(user.authUserId());
  }

  @GetMapping("/{id}")
  LearningEntryResponse get(CurrentUser user, @PathVariable UUID id) {
    return entries.get(user.authUserId(), id);
  }

  @PutMapping("/{id}")
  LearningEntryResponse replace(CurrentUser user, @PathVariable UUID id,
      @Valid @RequestBody LearningEntryRequest request) {
    return entries.replace(user.authUserId(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(CurrentUser user, @PathVariable UUID id) {
    entries.delete(user.authUserId(), id);
  }
}