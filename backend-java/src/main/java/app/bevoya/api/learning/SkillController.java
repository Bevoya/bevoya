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
@RequestMapping("/skills")
class SkillController {

  private final SkillService skills;

  SkillController(SkillService skills) {
    this.skills = skills;
  }

  @PostMapping
  ResponseEntity<SkillResponse> create(CurrentUser user,
      @RequestHeader(name = IdempotencyStore.HEADER, required = false)
      @Pattern(regexp = IdempotencyStore.KEY_PATTERN) String idempotencyKey,
      @Valid @RequestBody SkillRequest request) {
    SkillResponse created = skills.create(user.authUserId(), idempotencyKey, request);
    return ResponseEntity.created(URI.create("/skills/" + created.id())).body(created);
  }

  @GetMapping
  List<SkillResponse> list(CurrentUser user) {
    return skills.list(user.authUserId());
  }

  @GetMapping("/{id}")
  SkillResponse get(CurrentUser user, @PathVariable UUID id) {
    return skills.get(user.authUserId(), id);
  }

  @PutMapping("/{id}")
  SkillResponse replace(CurrentUser user, @PathVariable UUID id,
      @Valid @RequestBody SkillRequest request) {
    return skills.replace(user.authUserId(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(CurrentUser user, @PathVariable UUID id) {
    skills.delete(user.authUserId(), id);
  }
}