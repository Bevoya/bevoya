package app.bevoya.api.goals;

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
@RequestMapping("/goals/{goalId}/milestones")
class MilestoneController {

  private final MilestoneService milestones;

  MilestoneController(MilestoneService milestones) {
    this.milestones = milestones;
  }

  @PostMapping
  ResponseEntity<MilestoneResponse> create(CurrentUser user, @PathVariable UUID goalId,
      @RequestHeader(name = IdempotencyStore.HEADER, required = false)
      @Pattern(regexp = IdempotencyStore.KEY_PATTERN) String idempotencyKey,
      @Valid @RequestBody CreateMilestoneRequest request) {
    MilestoneResponse created = milestones.create(user.authUserId(), goalId, idempotencyKey, request);
    return ResponseEntity.created(URI.create("/goals/" + goalId + "/milestones/" + created.id()))
        .body(created);
  }

  @GetMapping
  List<MilestoneResponse> list(CurrentUser user, @PathVariable UUID goalId) {
    return milestones.list(user.authUserId(), goalId);
  }

  @PutMapping("/{id}")
  MilestoneResponse replace(CurrentUser user, @PathVariable UUID goalId, @PathVariable UUID id,
      @Valid @RequestBody ReplaceMilestoneRequest request) {
    return milestones.replace(user.authUserId(), goalId, id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(CurrentUser user, @PathVariable UUID goalId, @PathVariable UUID id) {
    milestones.delete(user.authUserId(), goalId, id);
  }

  @PutMapping("/order")
  List<MilestoneResponse> reorder(CurrentUser user, @PathVariable UUID goalId,
      @Valid @RequestBody ReorderMilestonesRequest request) {
    return milestones.reorder(user.authUserId(), goalId, request.milestoneIds());
  }
}