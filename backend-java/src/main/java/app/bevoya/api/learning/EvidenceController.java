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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/evidence")
class EvidenceController {

  private final EvidenceService evidence;

  EvidenceController(EvidenceService evidence) {
    this.evidence = evidence;
  }

  @PostMapping
  ResponseEntity<EvidenceResponse> create(CurrentUser user,
      @RequestHeader(name = IdempotencyStore.HEADER, required = false)
      @Pattern(regexp = IdempotencyStore.KEY_PATTERN) String idempotencyKey,
      @Valid @RequestBody CreateEvidenceRequest request) {
    EvidenceResponse created = evidence.create(user.authUserId(), idempotencyKey, request);
    return ResponseEntity.created(URI.create("/evidence/" + created.id())).body(created);
  }

  @GetMapping
  List<EvidenceResponse> list(CurrentUser user,
      @RequestParam(required = false) UUID sessionId,
      @RequestParam(required = false) @Pattern(regexp = EvidenceVocabulary.LEVEL_PATTERN)
          String activityLevel) {
    return evidence.list(user.authUserId(), sessionId, activityLevel);
  }

  @GetMapping("/{id}")
  EvidenceResponse get(CurrentUser user, @PathVariable UUID id) {
    return evidence.get(user.authUserId(), id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(CurrentUser user, @PathVariable UUID id) {
    evidence.delete(user.authUserId(), id);
  }
}