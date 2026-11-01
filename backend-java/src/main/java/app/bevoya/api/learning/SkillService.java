package app.bevoya.api.learning;

import app.bevoya.api.common.idempotency.IdempotencyStore;
import app.bevoya.api.common.web.ResourceNotFoundException;
import app.bevoya.api.identity.ProfileService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
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
 * JDBC with user_id in every predicate. The level is derived: the highest of the linked evidence
 * levels and "explained" when a linked learning entry exists. A skill that is missing and one that
 * is not yours give the same 404. Names are unique per person, ignoring case (409).
 */
@Service
class SkillService {

  private static final String CREATE_SCOPE = "skill.create";
  private static final int MAX_CREATE_ATTEMPTS = 3;
  private static final int LIST_LIMIT = 100;

  private static final String[] LEVELS =
      {"discovered", "studying", "explained", "practiced", "built", "tested", "applied"};

  private static final String EVIDENCE_RANK =
      "(select max(case e.activity_level when 'watched' then 1 when 'read' then 1 "
          + "when 'practiced' then 3 when 'built' then 4 when 'tested' then 5 when 'applied' then 6 end) "
          + "from public.evidence e where e.skill_id = s.id and e.user_id = s.user_id)";

  private static final String COLUMNS =
      "s.id, s.name, s.category, s.description, s.target_level, s.next_revision_at, "
          + "s.created_at, s.updated_at, "
          + "(select count(*) from public.evidence e where e.skill_id = s.id and e.user_id = s.user_id) "
          + "as evidence_count, "
          + "(select count(*) from public.learning_entries l where l.skill_id = s.id "
          + "and l.user_id = s.user_id) as note_count, "
          + "(select max(e.created_at) from public.evidence e where e.skill_id = s.id "
          + "and e.user_id = s.user_id) as last_practiced_at, "
          + "cast(greatest(coalesce(" + EVIDENCE_RANK + ", 0), "
          + "case when exists (select 1 from public.learning_entries l where l.skill_id = s.id "
          + "and l.user_id = s.user_id) then 2 else 0 end) as integer) as level_rank";

  private static final RowMapper<SkillResponse> MAPPER = (rs, n) -> new SkillResponse(
      rs.getObject("id", UUID.class),
      rs.getString("name"),
      rs.getString("category"),
      rs.getString("description"),
      LEVELS[rs.getInt("level_rank")],
      rs.getString("target_level"),
      rs.getInt("evidence_count"),
      rs.getInt("note_count"),
      instant(rs, "last_practiced_at"),
      instant(rs, "next_revision_at"),
      instant(rs, "created_at"),
      instant(rs, "updated_at"));

  private final JdbcClient jdbc;
  private final IdempotencyStore keys;
  private final ProfileService profiles;
  private final TransactionTemplate transaction;

  SkillService(JdbcClient jdbc, IdempotencyStore keys, ProfileService profiles,
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

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  private static String instantText(Instant value) {
    return value == null ? null : value.toString();
  }

  private static ErrorResponseException nameTaken() {
    return new ErrorResponseException(HttpStatus.CONFLICT,
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A skill with this name already exists."),
        null);
  }

  SkillResponse create(UUID userId, String idempotencyKey, SkillRequest request) {
    if (idempotencyKey == null) {
      return transaction.execute(status -> insert(userId, request));
    }
    for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
      Optional<SkillResponse> result =
          transaction.execute(status -> createOnce(status, userId, idempotencyKey, request));
      if (result.isPresent()) {
        return result.get();
      }
      // Lost a race with a simultaneous retry; the next attempt finds the winner's skill.
    }
    throw new IllegalStateException("Idempotent create did not settle");
  }

  private Optional<SkillResponse> createOnce(TransactionStatus status, UUID userId, String key,
      SkillRequest request) {
    Optional<UUID> earlier = keys.find(userId, CREATE_SCOPE, key);
    if (earlier.isPresent()) {
      Optional<SkillResponse> existing = byId(userId, earlier.get());
      if (existing.isPresent()) {
        return existing;
      }
      keys.release(userId, CREATE_SCOPE, key); // the skill was deleted since: the key is stale
    }
    SkillResponse created = insert(userId, request);
    if (!keys.claim(userId, CREATE_SCOPE, key, created.id())) {
      status.setRollbackOnly();
      return Optional.empty();
    }
    return Optional.of(created);
  }

  private SkillResponse insert(UUID userId, SkillRequest request) {
    profiles.ensureExists(userId);
    try {
      return jdbc.sql("with s as (insert into public.skills (id, user_id, name, category, description, "
              + "target_level, next_revision_at) "
              + "select cast(:id as uuid), p.id, cast(:name as text), cast(:category as text), "
              + "cast(:description as text), cast(:target as text), cast(:revision as timestamptz) "
              + "from public.profiles p where p.id = :user returning *) "
              + "select " + COLUMNS + " from s")
          .param("id", UUID.randomUUID())
          .param("name", request.name().strip(), Types.VARCHAR)
          .param("category", blankToNull(request.category()), Types.VARCHAR)
          .param("description", blankToNull(request.description()), Types.VARCHAR)
          .param("target", request.targetLevel(), Types.VARCHAR)
          .param("revision", instantText(request.nextRevisionAt()), Types.VARCHAR)
          .param("user", userId)
          .query(MAPPER)
          .optional()
          .orElseThrow(ResourceNotFoundException::new);
    } catch (DuplicateKeyException e) {
      throw nameTaken();
    }
  }

  SkillResponse get(UUID userId, UUID id) {
    return byId(userId, id).orElseThrow(ResourceNotFoundException::new);
  }

  private Optional<SkillResponse> byId(UUID userId, UUID id) {
    return jdbc.sql("select " + COLUMNS + " from public.skills s where s.id = :id and s.user_id = :user")
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional();
  }

  List<SkillResponse> list(UUID userId) {
    return jdbc.sql("select " + COLUMNS + " from public.skills s where s.user_id = :user "
            + "order by lower(s.name), s.id limit " + LIST_LIMIT)
        .param("user", userId)
        .query(MAPPER)
        .list();
  }

  /** PUT replaces everything; an omitted optional field is cleared. */
  SkillResponse replace(UUID userId, UUID id, SkillRequest request) {
    try {
      return jdbc.sql("with s as (update public.skills set name = cast(:name as text), "
              + "category = cast(:category as text), description = cast(:description as text), "
              + "target_level = cast(:target as text), next_revision_at = cast(:revision as timestamptz), "
              + "updated_at = now() where id = :id and user_id = :user returning *) "
              + "select " + COLUMNS + " from s")
          .param("name", request.name().strip(), Types.VARCHAR)
          .param("category", blankToNull(request.category()), Types.VARCHAR)
          .param("description", blankToNull(request.description()), Types.VARCHAR)
          .param("target", request.targetLevel(), Types.VARCHAR)
          .param("revision", instantText(request.nextRevisionAt()), Types.VARCHAR)
          .param("id", id)
          .param("user", userId)
          .query(MAPPER)
          .optional()
          .orElseThrow(ResourceNotFoundException::new);
    } catch (DuplicateKeyException e) {
      throw nameTaken();
    }
  }

  void delete(UUID userId, UUID id) {
    int deleted = jdbc.sql("delete from public.skills where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .update();
    if (deleted == 0) {
      throw new ResourceNotFoundException();
    }
  }
}