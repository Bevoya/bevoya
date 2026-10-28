package app.bevoya.api.goals;

import app.bevoya.api.common.idempotency.IdempotencyStore;
import app.bevoya.api.common.web.ResourceNotFoundException;
import app.bevoya.api.identity.ProfileService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class GoalService {

  private static final int LIST_LIMIT = 100;
  private static final String CREATE_SCOPE = "goal.create";
  private static final int MAX_CREATE_ATTEMPTS = 3;

  private final GoalRepository goals;
  private final ProfileService profiles;
  private final IdempotencyStore keys;
  private final TransactionTemplate transaction;
  private final Clock clock;

  GoalService(GoalRepository goals, ProfileService profiles, IdempotencyStore keys,
      PlatformTransactionManager transactionManager, Clock clock) {
    this.goals = goals;
    this.profiles = profiles;
    this.keys = keys;
    this.transaction = new TransactionTemplate(transactionManager);
    this.clock = clock;
  }

  /**
   * The profile insert (JDBC), the goal insert (JPA) and the key claim (JDBC) share one
   * transaction. With a key, a retry returns the goal the first request made.
   */
  public GoalResponse create(UUID userId, String idempotencyKey, CreateGoalRequest request) {
    if (idempotencyKey == null) {
      return transaction.execute(status -> {
        profiles.ensureExists(userId);
        Goal goal = newGoal(userId, request);
        goals.save(goal);
        return GoalResponse.from(goal);
      });
    }
    for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
      Optional<GoalResponse> result =
          transaction.execute(status -> createOnce(status, userId, idempotencyKey, request));
      if (result.isPresent()) {
        return result.get();
      }
      // Lost a race with a simultaneous retry; the next attempt finds the winner's goal.
    }
    throw new IllegalStateException("Idempotent create did not settle");
  }

  private Optional<GoalResponse> createOnce(TransactionStatus status, UUID userId, String key,
      CreateGoalRequest request) {
    profiles.ensureExists(userId);
    Optional<UUID> earlier = keys.find(userId, CREATE_SCOPE, key);
    if (earlier.isPresent()) {
      Optional<Goal> existing = goals.findByIdAndUserId(earlier.get(), userId);
      if (existing.isPresent()) {
        return Optional.of(GoalResponse.from(existing.get()));
      }
      keys.release(userId, CREATE_SCOPE, key); // the goal was deleted since: the key is stale
    }
    Goal goal = newGoal(userId, request);
    goals.save(goal); // the UUID is assigned here; the insert itself is flushed at commit
    if (!keys.claim(userId, CREATE_SCOPE, key, goal.getId())) {
      status.setRollbackOnly(); // discard our goal; the winner's goal is the answer
      return Optional.empty();
    }
    return Optional.of(GoalResponse.from(goal));
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

  private Goal newGoal(UUID userId, CreateGoalRequest request) {
    String status = request.status() == null ? "active" : request.status();
    return Goal.create(userId, request.title().strip(), blankToNull(request.description()),
        status, request.targetDate(), now());
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS); // Postgres keeps microseconds
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text;
  }
}