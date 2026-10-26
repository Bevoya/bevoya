package app.bevoya.api.identity;

import app.bevoya.api.common.security.UnknownAccountException;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** One insert-if-absent: one database round trip and no read-then-write race. */
@Service
class JdbcProfileService implements ProfileService {

  private static final String FOREIGN_KEY_VIOLATION = "23503";

  private final JdbcClient jdbc;

  JdbcProfileService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void ensureExists(UUID authUserId) {
    try {
      jdbc.sql("insert into public.profiles (id) values (:id) on conflict (id) do nothing")
          .param("id", authUserId)
          .update();
    } catch (DataIntegrityViolationException e) {
      // The only foreign key on this insert is profiles.id -> auth.users(id).
      if (isForeignKeyViolation(e)) {
        throw new UnknownAccountException();
      }
      throw e;
    }
  }

  private static boolean isForeignKeyViolation(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof SQLException sql && FOREIGN_KEY_VIOLATION.equals(sql.getSQLState())) {
        return true;
      }
    }
    return false;
  }
}