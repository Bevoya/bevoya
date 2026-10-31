package app.bevoya.api.sessions;

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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.ErrorResponseException;

/**
 * JDBC with user_id in every predicate. A session that is missing and a session that is not yours
 * give the same 404, and so does a task that is not yours. The timer comes from started_at, which
 * the database sets; the client clock is never used.
 */
@Service
class SessionService {

  private static final String CREATE_SCOPE = "session.create";
  private static final int MAX_CREATE_ATTEMPTS = 3;
  private static final int LIST_LIMIT = 100;

  private static final String COLUMNS =
      "id, task_id, activity_type, topic, started_at, ended_at, actual_minutes, focus_score, "
          + "output, notes, created_at";

  private static final RowMapper<SessionResponse> MAPPER = (rs, n) -> new SessionResponse(
      rs.getObject("id", UUID.class),
      rs.getObject("task_id", UUID.class),
      rs.getString("activity_type"),
      rs.getString("topic"),
      instant(rs, "started_at"),
      instant(rs, "ended_at"),
      rs.getObject("actual_minutes", Integer.class),
      rs.getObject("focus_score", Integer.class),
      rs.getString("output"),
      rs.getString("notes"),
      instant(rs, "created_at"));

  private final JdbcClient jdbc;
  private final IdempotencyStore keys;
  private final ProfileService profiles;
  private final TransactionTemplate transaction;

  SessionService(JdbcClient jdbc, IdempotencyStore keys, ProfileService profiles,
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

  SessionResponse start(UUID userId, String idempotencyKey, StartSessionRequest request) {
    try {
      return startOnce(userId, idempotencyKey, request);
    } catch (DuplicateKeyException e) {
      // The one-running-session-per-user index refused the insert.
      Optional<UUID> running = activeId(userId);
      if (running.isEmpty()) {
        throw e;
      }
      ProblemDetail problem =
          ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A session is already running");
      problem.setTitle("Conflict");
      problem.setProperty("activeSessionId", running.get().toString());
      throw new ErrorResponseException(HttpStatus.CONFLICT, problem, e);
    }
  }

  private SessionResponse startOnce(UUID userId, String idempotencyKey, StartSessionRequest request) {
    if (idempotencyKey == null) {
      return transaction.execute(status -> insert(userId, request));
    }
    for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
      Optional<SessionResponse> result =
          transaction.execute(status -> startWithKey(status, userId, idempotencyKey, request));
      if (result.isPresent()) {
        return result.get();
      }
      // Lost a race with a simultaneous retry; the next attempt finds the winner's session.
    }
    throw new IllegalStateException("Idempotent start did not settle");
  }

  private Optional<SessionResponse> startWithKey(TransactionStatus status, UUID userId, String key,
      StartSessionRequest request) {
    Optional<UUID> earlier = keys.find(userId, CREATE_SCOPE, key);
    if (earlier.isPresent()) {
      Optional<SessionResponse> existing = byId(userId, earlier.get());
      if (existing.isPresent()) {
        return existing;
      }
      keys.release(userId, CREATE_SCOPE, key); // the session was deleted since: the key is stale
    }
    SessionResponse created = insert(userId, request);
    if (!keys.claim(userId, CREATE_SCOPE, key, created.id())) {
      status.setRollbackOnly();
      return Optional.empty();
    }
    return Optional.of(created);
  }

  private SessionResponse insert(UUID userId, StartSessionRequest request) {
    profiles.ensureExists(userId);
    return jdbc.sql("insert into public.sessions (id, user_id, task_id, activity_type, topic) "
            + "select cast(:id as uuid), p.id, cast(:task as uuid), cast(:activity as text), "
            + "cast(:topic as text) "
            + "from public.profiles p where p.id = :user "
            + "and (cast(:task as uuid) is null or exists (select 1 from public.tasks t "
            + "where t.id = cast(:task as uuid) and t.user_id = :user)) "
            + "returning " + COLUMNS)
        .param("id", UUID.randomUUID())
        .param("task", request.taskId() == null ? null : request.taskId().toString(), Types.VARCHAR)
        .param("activity", request.activityType(), Types.VARCHAR)
        .param("topic", blankToNull(request.topic()), Types.VARCHAR)
        .param("user", userId)
        .query(MAPPER)
        .optional()
        .orElseThrow(ResourceNotFoundException::new);
  }

  SessionResponse complete(UUID userId, UUID id, CompleteSessionRequest request) {
    CompleteSessionRequest body = request == null ? CompleteSessionRequest.EMPTY : request;
    Optional<SessionResponse> done = jdbc.sql("update public.sessions set "
            + "ended_at = now(), "
            + "actual_minutes = cast(floor(extract(epoch from (now() - started_at)) / 60) as integer), "
            + "focus_score = cast(:focus as smallint), "
            + "output = cast(:output as text), "
            + "notes = cast(:notes as text) "
            + "where id = :id and user_id = :user and ended_at is null "
            + "returning " + COLUMNS)
        .param("focus", body.focusScore(), Types.INTEGER)
        .param("output", blankToNull(body.output()), Types.VARCHAR)
        .param("notes", blankToNull(body.notes()), Types.VARCHAR)
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional();
    if (done.isPresent()) {
      return done.get();
    }
    boolean yours = jdbc.sql("select exists (select 1 from public.sessions "
            + "where id = :id and user_id = :user)")
        .param("id", id)
        .param("user", userId)
        .query(Boolean.class)
        .single();
    if (!yours) {
      throw new ResourceNotFoundException();
    }
        ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "This session is already finished");
    problem.setTitle("Conflict");
    throw new ErrorResponseException(HttpStatus.CONFLICT, problem, null);
  }

  SessionResponse get(UUID userId, UUID id) {
    return byId(userId, id).orElseThrow(ResourceNotFoundException::new);
  }

  private Optional<SessionResponse> byId(UUID userId, UUID id) {
    return jdbc.sql("select " + COLUMNS + " from public.sessions where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional();
  }

  Optional<SessionResponse> active(UUID userId) {
    return jdbc.sql("select " + COLUMNS + " from public.sessions "
            + "where user_id = :user and ended_at is null")
        .param("user", userId)
        .query(MAPPER)
        .optional();
  }

  private Optional<UUID> activeId(UUID userId) {
    return jdbc.sql("select id from public.sessions where user_id = :user and ended_at is null")
        .param("user", userId)
        .query(UUID.class)
        .optional();
  }

  /** Newest first. {@code from} is read as the start of that day in UTC. */
  List<SessionResponse> list(UUID userId, LocalDate from) {
    OffsetDateTime since = from == null ? null : from.atStartOfDay().atOffset(ZoneOffset.UTC);
    return jdbc.sql("select " + COLUMNS + " from public.sessions where user_id = :user "
            + "and (cast(:since as timestamptz) is null or started_at >= cast(:since as timestamptz)) "
            + "order by started_at desc, id limit " + LIST_LIMIT)
        .param("user", userId)
        .param("since", since, Types.TIMESTAMP_WITH_TIMEZONE)
        .query(MAPPER)
        .list();
  }

  void delete(UUID userId, UUID id) {
    int deleted = jdbc.sql("delete from public.sessions where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .update();
    if (deleted == 0) {
      throw new ResourceNotFoundException();
    }
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text;
  }
}