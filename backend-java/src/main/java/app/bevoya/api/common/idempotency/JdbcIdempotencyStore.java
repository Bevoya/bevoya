package app.bevoya.api.common.idempotency;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
class JdbcIdempotencyStore implements IdempotencyStore {

  private final JdbcClient jdbc;

  JdbcIdempotencyStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<UUID> find(UUID userId, String scope, String key) {
    return jdbc.sql("select resource_id from public.idempotency_keys "
            + "where user_id = :user and scope = :scope and key = :key")
        .param("user", userId)
        .param("scope", scope)
        .param("key", key)
        .query(UUID.class)
        .optional();
  }

  @Override
  public boolean claim(UUID userId, String scope, String key, UUID resourceId) {
    // If another transaction is inserting the same key, this waits for it, then does nothing.
    int inserted = jdbc.sql("insert into public.idempotency_keys (user_id, scope, key, resource_id) "
            + "values (:user, :scope, :key, :resource) "
            + "on conflict (user_id, scope, key) do nothing")
        .param("user", userId)
        .param("scope", scope)
        .param("key", key)
        .param("resource", resourceId)
        .update();
    return inserted == 1;
  }

  @Override
  public void release(UUID userId, String scope, String key) {
    jdbc.sql("delete from public.idempotency_keys "
            + "where user_id = :user and scope = :scope and key = :key")
        .param("user", userId)
        .param("scope", scope)
        .param("key", key)
        .update();
  }
}