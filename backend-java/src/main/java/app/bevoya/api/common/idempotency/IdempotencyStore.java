package app.bevoya.api.common.idempotency;

import java.util.Optional;
import java.util.UUID;

/**
 * Remembers which resource an Idempotency-Key created, per user and scope, so a retried POST
 * returns the same resource instead of making a second one.
 */
public interface IdempotencyStore {

  String HEADER = "Idempotency-Key";

  /** 8 to 100 safe characters; a UUID fits. Matches the table's length check. */
  String KEY_PATTERN = "[A-Za-z0-9._:-]{8,100}";

  Optional<UUID> find(UUID userId, String scope, String key);

  /** Returns true if this call claimed the key, false if it already existed. */
  boolean claim(UUID userId, String scope, String key, UUID resourceId);

  void release(UUID userId, String scope, String key);
}