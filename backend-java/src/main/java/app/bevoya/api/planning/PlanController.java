package app.bevoya.api.planning;

import app.bevoya.api.common.security.CurrentUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plans")
class PlanController {

  private final TaskService tasks;

  PlanController(TaskService tasks) {
    this.tasks = tasks;
  }

  /** Tasks planned for today in the caller's own time zone. */
  @GetMapping("/today")
  List<TaskResponse> today(CurrentUser user) {
    return tasks.today(user.authUserId());
  }
}