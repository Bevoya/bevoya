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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JDBC with user_id in every predicate. Evidence is immutable: create, read, list, delete. A piece
 * of evidence that is missing, one that is not yours, and a session that is not yours all give the
 * same 404. When embeddings arrive (ADR-0004), delete must remove the chunks in the same transaction.
 */
@Service
class EvidenceService {

  private static final String CREATE_SCOPE = "evidence.create";
  private static final int MAX_CREATE_ATTEMPTS = 3;
  private static final int LIST_LIMIT = 100;

  private static final String COLUMNS =
      "id, session_id, skill_id, type, activity_level, title, content, external_url, created_at";

  private static final RowMapper<EvidenceResponse> MAPPER = (rs, n) -> new EvidenceResponse(
      rs.getObject("id", UUID.class),
      rs.getObject("session_id", UUID.class),
      rs.getObject("skill_id", UUID.class),
      rs.getString("type"),
      rs.getString("activity_level"),
      rs.getString("title"),
      rs.getString("content"),
      rs.getString("external_url"),
      instant(rs, "created_at"));

  private final JdbcClient jdbc;
  private final IdempotencyStore keys;
  private final ProfileService profiles;
  private final TransactionTemplate transaction;

  EvidenceService(JdbcClient jdbc, IdempotencyStore keys, ProfileService profiles,
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

  EvidenceResponse create(UUID userId, String idempotencyKey, CreateEvidenceRequest request) {
    if (idempotencyKey == null) {
      return transaction.execute(status -> insert(userId, request));
    }
    for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
      Optional<EvidenceResponse> result =
          transaction.execute(status -> createOnce(status, userId, idempotencyKey, request));
      if (result.isPresent()) {
        return result.get();
      }
      // Lost a race with a simultaneous retry; the next attempt finds the winner's evidence.
    }
    throw new IllegalStateException("Idempotent create did not settle");
  }

  private Optional<EvidenceResponse> createOnce(TransactionStatus status, UUID userId, String key,
      CreateEvidenceRequest request) {
    Optional<UUID> earlier = keys.find(userId, CREATE_SCOPE, key);
    if (earlier.isPresent()) {
      Optional<EvidenceResponse> existing = byId(userId, earlier.get());
      if (existing.isPresent()) {
        return existing;
      }
      keys.release(userId, CREATE_SCOPE, key); // the evidence was deleted since: the key is stale
    }
    EvidenceResponse created = insert(userId, request);
    if (!keys.claim(userId, CREATE_SCOPE, key, created.id())) {
      status.setRollbackOnly();
      return Optional.empty();
    }
    return Optional.of(created);
  }

  private EvidenceResponse insert(UUID userId, CreateEvidenceRequest request) {
    profiles.ensureExists(userId);
    return jdbc.sql("insert into public.evidence (id, user_id, session_id, skill_id, type, "
            + "activity_level, title, content, external_url) "
            + "select cast(:id as uuid), p.id, cast(:session as uuid), cast(:skill as uuid), "
            + "cast(:type as text), cast(:level as text), cast(:title as text), "
            + "cast(:content as text), cast(:url as text) "
            + "from public.profiles p where p.id = :user "
            + "and (cast(:session as uuid) is null or exists (select 1 from public.sessions s "
            + "where s.id = cast(:session as uuid) and s.user_id = :user)) "
            + "and (cast(:skill as uuid) is null or exists (select 1 from public.skills k "
            + "where k.id = cast(:skill as uuid) and k.user_id = :user)) "
            + "returning " + COLUMNS)
        .param("id", UUID.randomUUID())
        .param("session", request.sessionId() == null ? null : request.sessionId().toString(),
            Types.VARCHAR)
        .param("skill", request.skillId() == null ? null : request.skillId().toString(),
            Types.VARCHAR)
        .param("type", request.type(), Types.VARCHAR)
        .param("level", request.activityLevel(), Types.VARCHAR)
        .param("title", request.title().strip(), Types.VARCHAR)
        .param("content", blankToNull(request.content()), Types.VARCHAR)
        .param("url", blankToNull(request.externalUrl()), Types.VARCHAR)
        .param("user", userId)
        .query(MAPPER)
        .optional()
        .orElseThrow(ResourceNotFoundException::new);
  }

  EvidenceResponse get(UUID userId, UUID id) {
    return byId(userId, id).orElseThrow(ResourceNotFoundException::new);
  }

  private Optional<EvidenceResponse> byId(UUID userId, UUID id) {
    return jdbc.sql("select " + COLUMNS + " from public.evidence where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional();
  }

  List<EvidenceResponse> list(UUID userId, UUID sessionId, String activityLevel) {
    return jdbc.sql("select " + COLUMNS + " from public.evidence where user_id = :user "
            + "and (cast(:session as uuid) is null or session_id = cast(:session as uuid)) "
            + "and (cast(:level as text) is null or activity_level = cast(:level as text)) "
            + "order by created_at desc, id limit " + LIST_LIMIT)
        .param("user", userId)
        .param("session", sessionId == null ? null : sessionId.toString(), Types.VARCHAR)
        .param("level", activityLevel, Types.VARCHAR)
        .query(MAPPER)
        .list();
  }

  void delete(UUID userId, UUID id) {
    int deleted = jdbc.sql("delete from public.evidence where id = :id and user_id = :user")
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