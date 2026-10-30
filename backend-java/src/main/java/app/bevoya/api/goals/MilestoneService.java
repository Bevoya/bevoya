package app.bevoya.api.goals;

import app.bevoya.api.common.idempotency.IdempotencyStore;
import app.bevoya.api.common.web.ResourceNotFoundException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * All access is JDBC with user_id and goal_id in every predicate. A goal that is missing and a goal
 * that is not yours are the same thing here: nothing is found, so the answer is the same 404.
 */
@Service
class MilestoneService {

  private static final String CREATE_SCOPE = "milestone.create";
  private static final int MAX_CREATE_ATTEMPTS = 3;
  private static final String COLUMNS =
      "id, goal_id, title, description, status, target_date, position, created_at, updated_at";

  private static final RowMapper<MilestoneResponse> MAPPER = (rs, n) -> new MilestoneResponse(
      rs.getObject("id", UUID.class),
      rs.getObject("goal_id", UUID.class),
      rs.getString("title"),
      rs.getString("description"),
      rs.getString("status"),
      rs.getObject("target_date", LocalDate.class),
      rs.getInt("position"),
      rs.getObject("created_at", OffsetDateTime.class).toInstant(),
      rs.getObject("updated_at", OffsetDateTime.class).toInstant());

  private final JdbcClient jdbc;
  private final IdempotencyStore keys;
  private final TransactionTemplate transaction;

  MilestoneService(JdbcClient jdbc, IdempotencyStore keys, PlatformTransactionManager transactionManager) {
    this.jdbc = jdbc;
    this.keys = keys;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  MilestoneResponse create(UUID userId, UUID goalId, String idempotencyKey, CreateMilestoneRequest request) {
    if (idempotencyKey == null) {
      return transaction.execute(status -> insert(userId, goalId, request));
    }
    for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
      Optional<MilestoneResponse> result = transaction.execute(
          status -> createOnce(status, userId, goalId, idempotencyKey, request));
      if (result.isPresent()) {
        return result.get();
      }
      // Lost a race with a simultaneous retry; the next attempt finds the winner's milestone.
    }
    throw new IllegalStateException("Idempotent create did not settle");
  }

  private Optional<MilestoneResponse> createOnce(TransactionStatus status, UUID userId, UUID goalId,
      String key, CreateMilestoneRequest request) {
    Optional<UUID> earlier = keys.find(userId, CREATE_SCOPE, key);
    if (earlier.isPresent()) {
      Optional<MilestoneResponse> existing = byId(userId, earlier.get());
      if (existing.isPresent()) {
        if (!existing.get().goalId().equals(goalId)) {
          throw new ResponseStatusException(HttpStatus.CONFLICT,
              "This Idempotency-Key was already used for a different request");
        }
        return existing;
      }
      keys.release(userId, CREATE_SCOPE, key); // the milestone was deleted since: the key is stale
    }
    MilestoneResponse created = insert(userId, goalId, request);
    if (!keys.claim(userId, CREATE_SCOPE, key, created.id())) {
      status.setRollbackOnly(); // discard ours; the winner's milestone is the answer
      return Optional.empty();
    }
    return Optional.of(created);
  }

  /** One statement: inserts only if the goal is the caller's, and appends after the last position. */
  private MilestoneResponse insert(UUID userId, UUID goalId, CreateMilestoneRequest request) {
    String status = request.status() == null ? MilestoneStatus.DEFAULT : request.status();
    return jdbc.sql("insert into public.milestones "
            + "(id, user_id, goal_id, title, description, status, target_date, position) "
            + "select cast(:id as uuid), g.user_id, g.id, cast(:title as text), cast(:description as text), "
            + "cast(:status as text), cast(:target as date), "
            + "coalesce((select max(m.position) + 1 from public.milestones m "
            + "where m.goal_id = g.id and m.user_id = g.user_id), 0) "
            + "from public.goals g where g.id = :goal and g.user_id = :user "
            + "returning " + COLUMNS)
        .param("id", UUID.randomUUID())
        .param("title", request.title().strip(), Types.VARCHAR)
        .param("description", blankToNull(request.description()), Types.VARCHAR)
        .param("status", status, Types.VARCHAR)
        .param("target", request.targetDate(), Types.DATE)
        .param("goal", goalId)
        .param("user", userId)
        .query(MAPPER)
        .optional()
        .orElseThrow(ResourceNotFoundException::new);
  }

  private Optional<MilestoneResponse> byId(UUID userId, UUID id) {
    return jdbc.sql("select " + COLUMNS + " from public.milestones where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional();
  }

  /** One round trip. The left join tells "goal exists, no milestones" from "no such goal". */
  List<MilestoneResponse> list(UUID userId, UUID goalId) {
    List<Optional<MilestoneResponse>> rows = jdbc.sql(
            "select m.id as id, g.id as goal_id, m.title as title, m.description as description, "
                + "m.status as status, m.target_date as target_date, m.position as position, "
                + "m.created_at as created_at, m.updated_at as updated_at "
                + "from public.goals g left join public.milestones m "
                + "on m.goal_id = g.id and m.user_id = g.user_id "
                + "where g.id = :goal and g.user_id = :user "
                + "order by m.position, m.created_at, m.id")
        .param("goal", goalId)
        .param("user", userId)
        .query((rs, n) -> rs.getObject("id", UUID.class) == null
            ? Optional.<MilestoneResponse>empty()
            : Optional.of(MAPPER.mapRow(rs, n)))
        .list();
    if (rows.isEmpty()) {
      throw new ResourceNotFoundException();
    }
    return rows.stream().flatMap(Optional::stream).toList();
  }

  MilestoneResponse replace(UUID userId, UUID goalId, UUID id, ReplaceMilestoneRequest request) {
    return jdbc.sql("update public.milestones set title = :title, description = :description, "
            + "status = :status, target_date = :target, updated_at = now() "
            + "where id = :id and goal_id = :goal and user_id = :user returning " + COLUMNS)
        .param("title", request.title().strip(), Types.VARCHAR)
        .param("description", blankToNull(request.description()), Types.VARCHAR)
        .param("status", request.status(), Types.VARCHAR)
        .param("target", request.targetDate(), Types.DATE)
        .param("id", id)
        .param("goal", goalId)
        .param("user", userId)
        .query(MAPPER)
        .optional()
        .orElseThrow(ResourceNotFoundException::new);
  }

  void delete(UUID userId, UUID goalId, UUID id) {
    int deleted = jdbc.sql("delete from public.milestones "
            + "where id = :id and goal_id = :goal and user_id = :user")
        .param("id", id)
        .param("goal", goalId)
        .param("user", userId)
        .update();
    if (deleted == 0) {
      throw new ResourceNotFoundException();
    }
  }

  List<MilestoneResponse> reorder(UUID userId, UUID goalId, List<UUID> orderedIds) {
    return transaction.execute(status -> {
      List<MilestoneResponse> current = list(userId, goalId); // 404 if the goal is not yours
      HashSet<UUID> have = current.stream().map(MilestoneResponse::id)
          .collect(Collectors.toCollection(HashSet::new));
      if (orderedIds.size() != current.size() || !new HashSet<>(orderedIds).equals(have)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "milestoneIds must list every milestone of the goal exactly once");
      }
      if (!orderedIds.isEmpty()) {
        // The ids are parsed UUIDs, and travel as one bound text parameter, never as SQL text.
        jdbc.sql("update public.milestones m set position = cast(o.ord - 1 as integer), updated_at = now() "
                + "from unnest(string_to_array(cast(:ids as text), ',')) with ordinality as o(id, ord) "
                + "where m.id = cast(o.id as uuid) and m.goal_id = :goal and m.user_id = :user")
            .param("ids", orderedIds.stream().map(UUID::toString).collect(Collectors.joining(",")))
            .param("goal", goalId)
            .param("user", userId)
            .update();
      }
      return list(userId, goalId);
    });
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text;
  }
}