package app.bevoya.api.profiles;

import app.bevoya.api.common.idempotency.IdempotencyStore;
import app.bevoya.api.identity.ProfileService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class ConsentService {

  private static final String SCOPE = "consent.record";
  private static final int MAX_ATTEMPTS = 3;
  private static final String COLUMNS = "id, consent_type, granted, policy_version, recorded_at";

  private static final RowMapper<ConsentResponse> MAPPER = (rs, n) -> new ConsentResponse(
      rs.getObject("id", UUID.class),
      rs.getString("consent_type"),
      rs.getBoolean("granted"),
      rs.getString("policy_version"),
      rs.getObject("recorded_at", OffsetDateTime.class).toInstant());

  private final JdbcClient jdbc;
  private final ProfileService profiles;
  private final IdempotencyStore keys;
  private final TransactionTemplate transaction;

  ConsentService(JdbcClient jdbc, ProfileService profiles, IdempotencyStore keys,
      PlatformTransactionManager transactionManager) {
    this.jdbc = jdbc;
    this.profiles = profiles;
    this.keys = keys;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  /** The newest row for each consent type. A person with no profile yet simply has none. */
  List<ConsentResponse> current(UUID userId) {
    return jdbc.sql("select distinct on (consent_type) " + COLUMNS
            + " from public.consent_records where user_id = :user "
            + "order by consent_type, recorded_at desc, id")
        .param("user", userId)
        .query(MAPPER)
        .list();
  }

  ConsentResponse record(UUID userId, String idempotencyKey, RecordConsentRequest request) {
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      Optional<ConsentResponse> result = transaction.execute(
          status -> recordOnce(status, userId, idempotencyKey, request));
      if (result.isPresent()) {
        return result.get();
      }
      // Lost a race with a simultaneous retry; the next attempt finds the winner's row.
    }
    throw new IllegalStateException("Idempotent consent did not settle");
  }

  private Optional<ConsentResponse> recordOnce(TransactionStatus status, UUID userId, String key,
      RecordConsentRequest request) {
    profiles.ensureExists(userId);
    if (key != null) {
      Optional<UUID> earlier = keys.find(userId, SCOPE, key);
      if (earlier.isPresent()) {
        Optional<ConsentResponse> existing = byId(earlier.get(), userId);
        if (existing.isPresent()) {
          return existing;
        }
        keys.release(userId, SCOPE, key);
      }
    }
    ConsentResponse created =
        insert(userId, request.consentType(), request.granted(), request.policyVersion());
    if (key != null && !keys.claim(userId, SCOPE, key, created.id())) {
      status.setRollbackOnly();
      return Optional.empty();
    }
    return Optional.of(created);
  }

  AiSettingsResponse aiSettings(UUID userId) {
    boolean memory = false;
    boolean journal = false;
    for (ConsentResponse consent : current(userId)) {
      if (consent.consentType().equals("ai_memory")) {
        memory = consent.granted();
      } else if (consent.consentType().equals("journal_retrieval")) {
        journal = consent.granted();
      }
    }
    return new AiSettingsResponse(memory, journal);
  }

  /** Writes a consent row only for a setting that actually changes. */
  @Transactional
  AiSettingsResponse updateAiSettings(UUID userId, AiSettingsRequest request) {
    profiles.ensureExists(userId);
    AiSettingsResponse now = aiSettings(userId);
    if (request.aiMemoryEnabled() != now.aiMemoryEnabled()) {
      insert(userId, "ai_memory", request.aiMemoryEnabled(), request.policyVersion());
    }
    if (request.journalRetrievalEnabled() != now.journalRetrievalEnabled()) {
      insert(userId, "journal_retrieval", request.journalRetrievalEnabled(), request.policyVersion());
    }
    return new AiSettingsResponse(request.aiMemoryEnabled(), request.journalRetrievalEnabled());
  }

  private Optional<ConsentResponse> byId(UUID id, UUID userId) {
    return jdbc.sql("select " + COLUMNS + " from public.consent_records "
            + "where id = :id and user_id = :user")
        .param("id", id)
        .param("user", userId)
        .query(MAPPER)
        .optional();
  }

  private ConsentResponse insert(UUID userId, String type, boolean granted, String policyVersion) {
    UUID id = UUID.randomUUID();
    Instant recordedAt = jdbc.sql("insert into public.consent_records "
            + "(id, user_id, consent_type, granted, policy_version) "
            + "values (:id, :user, :type, :granted, :version) returning recorded_at")
        .param("id", id)
        .param("user", userId)
        .param("type", type)
        .param("granted", granted)
        .param("version", policyVersion)
        .query((rs, n) -> rs.getObject("recorded_at", OffsetDateTime.class).toInstant())
        .single();
    return new ConsentResponse(id, type, granted, policyVersion, recordedAt);
  }
}