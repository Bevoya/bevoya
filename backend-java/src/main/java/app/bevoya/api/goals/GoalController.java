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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/goals")
class GoalController {

  private final GoalService goals;

  GoalController(GoalService goals) {
    this.goals = goals;
  }

  @PostMapping
  ResponseEntity<GoalResponse> create(CurrentUser user,
      @RequestHeader(name = IdempotencyStore.HEADER, required = false)
      @Pattern(regexp = IdempotencyStore.KEY_PATTERN) String idempotencyKey,
      @Valid @RequestBody CreateGoalRequest request) {
    GoalResponse created = goals.create(user.authUserId(), idempotencyKey, request);
    return ResponseEntity.created(URI.create("/goals/" + created.id())).body(created);
  }

  @GetMapping
  List<GoalResponse> list(CurrentUser user,
      @RequestParam(required = false) @Pattern(regexp = Goal.STATUS_PATTERN) String status) {
    return goals.list(user.authUserId(), status);
  }

  @GetMapping("/{id}")
  GoalResponse get(CurrentUser user, @PathVariable UUID id) {
    return goals.get(user.authUserId(), id);
  }

  @PutMapping("/{id}")
  GoalResponse replace(CurrentUser user, @PathVariable UUID id,
      @Valid @RequestBody ReplaceGoalRequest request) {
    return goals.replace(user.authUserId(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(CurrentUser user, @PathVariable UUID id) {
    goals.delete(user.authUserId(), id);
  }
}