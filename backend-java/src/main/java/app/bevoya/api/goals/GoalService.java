package app.bevoya.api.goals;

import app.bevoya.api.common.web.ResourceNotFoundException;
import app.bevoya.api.identity.ProfileService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GoalService {

  private static final int LIST_LIMIT = 100;

  private final GoalRepository goals;
  private final ProfileService profiles;
  private final Clock clock;

  GoalService(GoalRepository goals, ProfileService profiles, Clock clock) {
    this.goals = goals;
    this.profiles = profiles;
    this.clock = clock;
  }

  /** The profile insert (JDBC) and the goal insert (JPA) share one transaction. */
  @Transactional
  public GoalResponse create(UUID userId, CreateGoalRequest request) {
    profiles.ensureExists(userId);
    String status = request.status() == null ? "active" : request.status();
    Goal goal = Goal.create(userId, request.title().strip(), blankToNull(request.description()),
        status, request.targetDate(), now());
    goals.save(goal);
    return GoalResponse.from(goal);
  }

  // Reads have no @Transactional on purpose: one SELECT is one round trip,
  // and BEGIN/COMMIT around it would add another.
  public List<GoalResponse> list(UUID userId, String status) {
    Limit limit = Limit.of(LIST_LIMIT);
    List<Goal> found = status == null
        ? goals.findByUserIdOrderByCreatedAtDesc(userId, limit)
        : goals.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, limit);
    return found.stream().map(GoalResponse::from).toList();
  }

  public GoalResponse get(UUID userId, UUID goalId) {
    return goals.findByIdAndUserId(goalId, userId)
        .map(GoalResponse::from)
        .orElseThrow(ResourceNotFoundException::new);
  }

  @Transactional
  public GoalResponse replace(UUID userId, UUID goalId, ReplaceGoalRequest request) {
    Goal goal = goals.findByIdAndUserId(goalId, userId).orElseThrow(ResourceNotFoundException::new);
    goal.replace(request.title().strip(), blankToNull(request.description()), request.status(),
        request.targetDate(), now());
    return GoalResponse.from(goal); // flushed by dirty checking at commit
  }

  @Transactional
  public void delete(UUID userId, UUID goalId) {
    if (goals.deleteOwned(goalId, userId) == 0) {
      throw new ResourceNotFoundException();
    }
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS); // Postgres keeps microseconds
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text;
  }
}