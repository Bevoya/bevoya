package app.bevoya.api.reflections;

import app.bevoya.api.common.idempotency.IdempotencyStore;
import app.bevoya.api.common.web.ResourceNotFoundException;
import app.bevoya.api.identity.ProfileService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
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
 * Private journal, JDBC with user_id in every predicate. A missing entry and one that is not yours
 * give the same 404. The text is the most sensitive data we hold: never log it, never put it in an
 * exception message. When embeddings arrive (ADR-0004), update and delete must refresh or remove the
 * chunks in the same transaction, and only with the journal-retrieval consent.
 */
@Service
class ReflectionService {

  private static final String CREATE_SCOPE = "reflection.create";
  private static final int MAX_CREATE_ATTEMPTS = 3;
  private static final int LIST_LIMIT = 100;

  private static final String COLUMNS =
      "id, entry_date, raw_text, mood, energy, created_at, updated_at";

  private static final RowMapper<ReflectionResponse> MAPPER = (rs, n) -> new ReflectionResponse(
      rs.getObject("id", UUID.class),
      rs.getObject("entry_date", LocalDate.class),
      rs.getString("raw_text"),
      rs.getObject("mood", Integer.class),
      rs.getObject("energy", Integer.class),
      instant(rs, "created_at"),
      instant(rs, "updated_at"));

  private final JdbcClient jdbc;
  private final IdempotencyStore keys;
  private final ProfileService profiles;
  private final TransactionTemplate transaction;

  ReflectionService(JdbcClient jdbc, IdempotencyStore keys, ProfileService profiles,
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

  private static String dateText(LocalDate date) {
    return date == null ? null : date.toString();
  }

  ReflectionResponse create(UUID userId, String idempotencyKey, ReflectionRequest request) {
    if (idempotencyKey == null) {
      return transaction.execute(status -> insert(userId, request));
    }
    for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
      Optional<ReflectionResponse> result =
          transaction.execute(status -> createOnce(status, userId, idempotencyKey, request));
      if (result.isPresent()) {
        return result.get();
      }
      // Lost a race with a simultaneous retry; the next attempt finds the winner's entry.
    }
    throw new IllegalStateException("Idempotent create did not settle");
  }

  private Optional<ReflectionResponse> createOnce(TransactionStatus status, UUID userId,
      String key, ReflectionRequest request) {
    Optional<UUID> earlier = keys.find(userId, CREATE_SCOPE, key);
    if (earlier.isPresent()) {
      Optional<ReflectionResponse> existing = byId(userId, earlier.get());
      if (existing.isPresent()) {
        return existing;
      }
      keys.release(userId, CREATE_SCOPE, key); // the entry was deleted since: the key is stale
    }
    ReflectionResponse created = insert(userId, request);
    if (!keys.claim(userId, CREATE_SCOPE, key, created.id())) {
      status.setRollbackOnly();
      return Optional.empty();
    }
    return Optional.of(created);
  }

  private ReflectionResponse insert(UUID userId, ReflectionRequest request) {
    profiles.ensureExists(userId);
    // No date given: today in the person's own time zone, worked out in the database.
    return jdbc.sql("insert into public.reflections (id, user_id, entry_date, raw_text, mood, energy) "
            + "select cast(:id as uuid), p.id, "
            + "coalesce(cast(:date as date), (now() at time zone p.time_zone)::date), "
            + "cast(:text as text), cast(:mood as smallint), cast(:energy as smallint) "
            + "from public.profiles p where p.id = :user "
            + "returning " + COLUMNS)
        .param("id", UUID.randomUUID())
        .param("date", dateText(request.entryDate()), Types.VARCHAR)
        .param("text", request.rawText(), Types.VARCHAR)
        .param("mood", request.mood(), Types.INTEGER)
        .param("energy", request.energy(), Types.INTEGER)
        .param("user", userId)
        .query(MAPPER)
        .optional()
        .orElseThrow(ResourceNotFoundException::new);
  }

  ReflectionResponse get(UUID userId, UUID id) {
    return byId(userId, id).orElseThrow(ResourceNotFoundException::new);
  }

  private Optional<ReflectionResponse> byId(UUID userId, UUID id) {
    return jdbc.sql("select " + COLUMNS + " from public.reflections "
            + "where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional();
  }

  List<ReflectionResponse> list(UUID userId, LocalDate from) {
    return jdbc.sql("select " + COLUMNS + " from public.reflections where user_id = :user "
            + "and (cast(:from as date) is null or entry_date >= cast(:from as date)) "
            + "order by entry_date desc, created_at desc, id limit " + LIST_LIMIT)
        .param("user", userId)
        .param("from", dateText(from), Types.VARCHAR)
        .query(MAPPER)
        .list();
  }

  /** PUT replaces text, mood and energy (omitted mood or energy is cleared); an omitted date keeps the old one. */
  ReflectionResponse replace(UUID userId, UUID id, ReflectionRequest request) {
    return jdbc.sql("update public.reflections set "
            + "entry_date = coalesce(cast(:date as date), entry_date), "
            + "raw_text = cast(:text as text), mood = cast(:mood as smallint), "
            + "energy = cast(:energy as smallint), updated_at = now() "
            + "where id = :id and user_id = :user returning " + COLUMNS)
        .param("date", dateText(request.entryDate()), Types.VARCHAR)
        .param("text", request.rawText(), Types.VARCHAR)
        .param("mood", request.mood(), Types.INTEGER)
        .param("energy", request.energy(), Types.INTEGER)
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional()
        .orElseThrow(ResourceNotFoundException::new);
  }

  void delete(UUID userId, UUID id) {
    int deleted = jdbc.sql("delete from public.reflections where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .update();
    if (deleted == 0) {
      throw new ResourceNotFoundException();
    }
  }
}