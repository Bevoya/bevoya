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
 * JDBC with user_id in every predicate. A learning entry that is missing and one that is not yours
 * give the same 404. PUT replaces everything. When embeddings arrive (ADR-0004), update and delete
 * must refresh or remove the chunks in the same transaction.
 */
@Service
class LearningEntryService {

  private static final String CREATE_SCOPE = "learning.create";
  private static final int MAX_CREATE_ATTEMPTS = 3;
  private static final int LIST_LIMIT = 100;

  private static final String COLUMNS =
      "id, title, content, source_url, confidence, created_at, updated_at";

  private static final RowMapper<LearningEntryResponse> MAPPER = (rs, n) -> new LearningEntryResponse(
      rs.getObject("id", UUID.class),
      rs.getString("title"),
      rs.getString("content"),
      rs.getString("source_url"),
      rs.getObject("confidence", Integer.class),
      instant(rs, "created_at"),
      instant(rs, "updated_at"));

  private final JdbcClient jdbc;
  private final IdempotencyStore keys;
  private final ProfileService profiles;
  private final TransactionTemplate transaction;

  LearningEntryService(JdbcClient jdbc, IdempotencyStore keys, ProfileService profiles,
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

  LearningEntryResponse create(UUID userId, String idempotencyKey, LearningEntryRequest request) {
    if (idempotencyKey == null) {
      return transaction.execute(status -> insert(userId, request));
    }
    for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
      Optional<LearningEntryResponse> result =
          transaction.execute(status -> createOnce(status, userId, idempotencyKey, request));
      if (result.isPresent()) {
        return result.get();
      }
      // Lost a race with a simultaneous retry; the next attempt finds the winner's entry.
    }
    throw new IllegalStateException("Idempotent create did not settle");
  }

  private Optional<LearningEntryResponse> createOnce(TransactionStatus status, UUID userId,
      String key, LearningEntryRequest request) {
    Optional<UUID> earlier = keys.find(userId, CREATE_SCOPE, key);
    if (earlier.isPresent()) {
      Optional<LearningEntryResponse> existing = byId(userId, earlier.get());
      if (existing.isPresent()) {
        return existing;
      }
      keys.release(userId, CREATE_SCOPE, key); // the entry was deleted since: the key is stale
    }
    LearningEntryResponse created = insert(userId, request);
    if (!keys.claim(userId, CREATE_SCOPE, key, created.id())) {
      status.setRollbackOnly();
      return Optional.empty();
    }
    return Optional.of(created);
  }

  private LearningEntryResponse insert(UUID userId, LearningEntryRequest request) {
    profiles.ensureExists(userId);
    return jdbc.sql("insert into public.learning_entries (id, user_id, title, content, source_url, "
            + "confidence) "
            + "select cast(:id as uuid), p.id, cast(:title as text), cast(:content as text), "
            + "cast(:url as text), cast(:confidence as smallint) "
            + "from public.profiles p where p.id = :user "
            + "returning " + COLUMNS)
        .param("id", UUID.randomUUID())
        .param("title", request.title().strip(), Types.VARCHAR)
        .param("content", request.content(), Types.VARCHAR)
        .param("url", blankToNull(request.sourceUrl()), Types.VARCHAR)
        .param("confidence", request.confidence(), Types.INTEGER)
        .param("user", userId)
        .query(MAPPER)
        .optional()
        .orElseThrow(ResourceNotFoundException::new);
  }

  LearningEntryResponse get(UUID userId, UUID id) {
    return byId(userId, id).orElseThrow(ResourceNotFoundException::new);
  }

  private Optional<LearningEntryResponse> byId(UUID userId, UUID id) {
    return jdbc.sql("select " + COLUMNS + " from public.learning_entries "
            + "where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional();
  }

  List<LearningEntryResponse> list(UUID userId) {
    return jdbc.sql("select " + COLUMNS + " from public.learning_entries where user_id = :user "
            + "order by created_at desc, id limit " + LIST_LIMIT)
        .param("user", userId)
        .query(MAPPER)
        .list();
  }

  LearningEntryResponse replace(UUID userId, UUID id, LearningEntryRequest request) {
    return jdbc.sql("update public.learning_entries set "
            + "title = cast(:title as text), content = cast(:content as text), "
            + "source_url = cast(:url as text), confidence = cast(:confidence as smallint), "
            + "updated_at = now() "
            + "where id = :id and user_id = :user returning " + COLUMNS)
        .param("title", request.title().strip(), Types.VARCHAR)
        .param("content", request.content(), Types.VARCHAR)
        .param("url", blankToNull(request.sourceUrl()), Types.VARCHAR)
        .param("confidence", request.confidence(), Types.INTEGER)
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional()
        .orElseThrow(ResourceNotFoundException::new);
  }

  void delete(UUID userId, UUID id) {
    int deleted = jdbc.sql("delete from public.learning_entries where id = :id and user_id = :user")
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