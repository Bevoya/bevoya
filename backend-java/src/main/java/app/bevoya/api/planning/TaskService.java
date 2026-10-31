package app.bevoya.api.planning;

import app.bevoya.api.common.idempotency.IdempotencyStore;
import app.bevoya.api.common.web.ResourceNotFoundException;
import app.bevoya.api.identity.ProfileService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JDBC with user_id in every predicate. A task that is missing, a task that is not yours, and a
 * goal or milestone that is not yours all give the same 404.
 */
@Service
class TaskService {

  private static final String CREATE_SCOPE = "task.create";
  private static final int MAX_CREATE_ATTEMPTS = 3;
  private static final int LIST_LIMIT = 100;

  private static final String COLUMNS =
      "id, goal_id, milestone_id, title, description, category, priority, estimated_minutes, "
          + "planned_for, due_at, status, why_it_matters, completed_at, created_at, updated_at";

  private static final String T_COLUMNS = "t.id, t.goal_id, t.milestone_id, t.title, t.description, "
      + "t.category, t.priority, t.estimated_minutes, t.planned_for, t.due_at, t.status, "
      + "t.why_it_matters, t.completed_at, t.created_at, t.updated_at";

  /** The goal and milestone, when given, must belong to the caller (and the milestone to the goal). */
  private static final String LINKS_OK =
      "(cast(:goal as uuid) is null or exists (select 1 from public.goals g "
          + "where g.id = cast(:goal as uuid) and g.user_id = :user)) "
          + "and (cast(:milestone as uuid) is null or exists (select 1 from public.milestones m "
          + "where m.id = cast(:milestone as uuid) and m.user_id = :user "
          + "and (cast(:goal as uuid) is null or m.goal_id = cast(:goal as uuid))))";

  private static final RowMapper<TaskResponse> MAPPER = (rs, n) -> new TaskResponse(
      rs.getObject("id", UUID.class),
      rs.getObject("goal_id", UUID.class),
      rs.getObject("milestone_id", UUID.class),
      rs.getString("title"),
      rs.getString("description"),
      rs.getString("category"),
      rs.getInt("priority"),
      rs.getObject("estimated_minutes", Integer.class),
      rs.getObject("planned_for", LocalDate.class),
      instant(rs, "due_at"),
      rs.getString("status"),
      rs.getString("why_it_matters"),
      instant(rs, "completed_at"),
      instant(rs, "created_at"),
      instant(rs, "updated_at"));

  private final JdbcClient jdbc;
  private final IdempotencyStore keys;
  private final ProfileService profiles;
  private final TransactionTemplate transaction;

  TaskService(JdbcClient jdbc, IdempotencyStore keys, ProfileService profiles,
      PlatformTransactionManager transactionManager) {
    this.jdbc = jdbc;
    this.keys = keys;
    this.profiles = profiles;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  TaskResponse create(UUID userId, String idempotencyKey, CreateTaskRequest request) {
    if (idempotencyKey == null) {
      return transaction.execute(status -> insert(userId, request));
    }
    for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
      Optional<TaskResponse> result =
          transaction.execute(status -> createOnce(status, userId, idempotencyKey, request));
      if (result.isPresent()) {
        return result.get();
      }
      // Lost a race with a simultaneous retry; the next attempt finds the winner's task.
    }
    throw new IllegalStateException("Idempotent create did not settle");
  }

  private Optional<TaskResponse> createOnce(TransactionStatus status, UUID userId, String key,
      CreateTaskRequest request) {
    Optional<UUID> earlier = keys.find(userId, CREATE_SCOPE, key);
    if (earlier.isPresent()) {
      Optional<TaskResponse> existing = byId(userId, earlier.get());
      if (existing.isPresent()) {
        return existing;
      }
      keys.release(userId, CREATE_SCOPE, key); // the task was deleted since: the key is stale
    }
    TaskResponse created = insert(userId, request);
    if (!keys.claim(userId, CREATE_SCOPE, key, created.id())) {
      status.setRollbackOnly();
      return Optional.empty();
    }
    return Optional.of(created);
  }

  private TaskResponse insert(UUID userId, CreateTaskRequest request) {
    profiles.ensureExists(userId);
    String status = request.status() == null ? TaskStatus.DEFAULT : request.status();
    int priority = request.priority() == null ? 2 : request.priority();
    return jdbc.sql("insert into public.tasks (id, user_id, goal_id, milestone_id, title, description, "
            + "category, priority, estimated_minutes, planned_for, due_at, status, why_it_matters, "
            + "completed_at) "
            + "select cast(:id as uuid), p.id, cast(:goal as uuid), cast(:milestone as uuid), "
            + "cast(:title as text), cast(:description as text), cast(:category as text), "
            + "cast(:priority as smallint), cast(:minutes as integer), cast(:planned as date), "
            + "cast(:due as timestamptz), cast(:status as text), cast(:why as text), "
            + "case when cast(:status as text) = 'done' then now() else null end "
            + "from public.profiles p where p.id = :user and " + LINKS_OK
            + " returning " + COLUMNS)
        .param("id", UUID.randomUUID())
        .param("goal", uuidText(request.goalId()), Types.VARCHAR)
        .param("milestone", uuidText(request.milestoneId()), Types.VARCHAR)
        .param("title", request.title().strip(), Types.VARCHAR)
        .param("description", blankToNull(request.description()), Types.VARCHAR)
        .param("category", request.category(), Types.VARCHAR)
        .param("priority", priority, Types.INTEGER)
        .param("minutes", request.estimatedMinutes(), Types.INTEGER)
        .param("planned", request.plannedFor(), Types.DATE)
        .param("due", offset(request.dueAt()), Types.TIMESTAMP_WITH_TIMEZONE)
        .param("status", status, Types.VARCHAR)
        .param("why", blankToNull(request.whyItMatters()), Types.VARCHAR)
        .param("user", userId)
        .query(MAPPER)
        .optional()
        .orElseThrow(ResourceNotFoundException::new);
  }

  TaskResponse get(UUID userId, UUID id) {
    return byId(userId, id).orElseThrow(ResourceNotFoundException::new);
  }

  private Optional<TaskResponse> byId(UUID userId, UUID id) {
    return jdbc.sql("select " + COLUMNS + " from public.tasks where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional();
  }

  List<TaskResponse> list(UUID userId, LocalDate date, String status, UUID goalId) {
    return jdbc.sql("select " + COLUMNS + " from public.tasks where user_id = :user "
            + "and (cast(:date as date) is null or planned_for = cast(:date as date)) "
            + "and (cast(:status as text) is null or status = cast(:status as text)) "
            + "and (cast(:goal as uuid) is null or goal_id = cast(:goal as uuid)) "
            + "order by planned_for nulls last, priority, created_at, id limit " + LIST_LIMIT)
        .param("user", userId)
        .param("date", date, Types.DATE)
        .param("status", status, Types.VARCHAR)
        .param("goal", uuidText(goalId), Types.VARCHAR)
        .query(MAPPER)
        .list();
  }

  /** One round trip: "today" is computed in the database from the user's own time zone. */
  List<TaskResponse> today(UUID userId) {
    return jdbc.sql("select " + T_COLUMNS + " from public.tasks t "
            + "join public.profiles p on p.id = t.user_id "
            + "where t.user_id = :user "
            + "and t.planned_for = cast((now() at time zone p.time_zone) as date) "
            + "order by (t.status in ('done', 'skipped')), t.priority, t.created_at, t.id")
        .param("user", userId)
        .query(MAPPER)
        .list();
  }

  TaskResponse update(UUID userId, UUID id, UpdateTaskRequest request) {
    return jdbc.sql("update public.tasks set "
            + "title = coalesce(cast(:title as text), title), "
            + "description = coalesce(cast(:description as text), description), "
            + "category = coalesce(cast(:category as text), category), "
            + "priority = coalesce(cast(:priority as smallint), priority), "
            + "estimated_minutes = coalesce(cast(:minutes as integer), estimated_minutes), "
            + "planned_for = coalesce(cast(:planned as date), planned_for), "
            + "due_at = coalesce(cast(:due as timestamptz), due_at), "
            + "why_it_matters = coalesce(cast(:why as text), why_it_matters), "
            + "goal_id = coalesce(cast(:goal as uuid), goal_id), "
            + "milestone_id = coalesce(cast(:milestone as uuid), milestone_id), "
            + "completed_at = case when cast(:status as text) is null then completed_at "
            + "when cast(:status as text) = 'done' then coalesce(completed_at, now()) else null end, "
            + "status = coalesce(cast(:status as text), status), "
            + "updated_at = now() "
            + "where id = :id and user_id = :user and " + LINKS_OK
            + " returning " + COLUMNS)
        .param("title", request.title() == null ? null : request.title().strip(), Types.VARCHAR)
        .param("description", request.description(), Types.VARCHAR)
        .param("category", request.category(), Types.VARCHAR)
        .param("priority", request.priority(), Types.INTEGER)
        .param("minutes", request.estimatedMinutes(), Types.INTEGER)
        .param("planned", request.plannedFor(), Types.DATE)
        .param("due", offset(request.dueAt()), Types.TIMESTAMP_WITH_TIMEZONE)
        .param("why", request.whyItMatters(), Types.VARCHAR)
        .param("goal", uuidText(request.goalId()), Types.VARCHAR)
        .param("milestone", uuidText(request.milestoneId()), Types.VARCHAR)
        .param("status", request.status(), Types.VARCHAR)
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional()
        .orElseThrow(ResourceNotFoundException::new);
  }

  void delete(UUID userId, UUID id) {
    int deleted = jdbc.sql("delete from public.tasks where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .update();
    if (deleted == 0) {
      throw new ResourceNotFoundException();
    }
  }

  private static String uuidText(UUID id) {
    return id == null ? null : id.toString();
  }

  private static OffsetDateTime offset(Instant instant) {
    return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text;
  }
}