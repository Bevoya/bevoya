package app.bevoya.api.identity;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** One insert-if-absent: one database round trip and no read-then-write race. */
@Service
class JdbcProfileService implements ProfileService {

  private final JdbcClient jdbc;

  JdbcProfileService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void ensureExists(UUID authUserId) {
    jdbc.sql("insert into public.profiles (id) values (:id) on conflict (id) do nothing")
        .param("id", authUserId)
        .update();
  }
}