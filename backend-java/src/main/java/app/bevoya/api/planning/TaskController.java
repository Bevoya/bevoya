package app.bevoya.api.planning;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/tasks")
class TaskController {

  private final TaskService tasks;

  TaskController(TaskService tasks) {
    this.tasks = tasks;
  }

  @PostMapping
  ResponseEntity<TaskResponse> create(CurrentUser user,
      @RequestHeader(name = IdempotencyStore.HEADER, required = false)
      @Pattern(regexp = IdempotencyStore.KEY_PATTERN) String idempotencyKey,
      @Valid @RequestBody CreateTaskRequest request) {
    TaskResponse created = tasks.create(user.authUserId(), idempotencyKey, request);
    return ResponseEntity.created(URI.create("/tasks/" + created.id())).body(created);
  }

  @GetMapping
  List<TaskResponse> list(CurrentUser user,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) @Pattern(regexp = TaskStatus.PATTERN) String status,
      @RequestParam(required = false) UUID goalId) {
    return tasks.list(user.authUserId(), date, status, goalId);
  }

  @GetMapping("/{id}")
  TaskResponse get(CurrentUser user, @PathVariable UUID id) {
    return tasks.get(user.authUserId(), id);
  }

  @PatchMapping("/{id}")
  TaskResponse update(CurrentUser user, @PathVariable UUID id,
      @Valid @RequestBody UpdateTaskRequest request) {
    return tasks.update(user.authUserId(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(CurrentUser user, @PathVariable UUID id) {
    tasks.delete(user.authUserId(), id);
  }
}