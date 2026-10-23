package app.bevoya.api.common.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseStartupCheckTest {

    /** Fails the test if the check touches the database at all. */
    private static final class ForbiddenJdbc extends JdbcTemplate {
        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType) {
            throw new AssertionError("The database must not be queried when the check is disabled");
        }
    }

    /** Behaves like an unreachable database. */
    private static final class DownJdbc extends JdbcTemplate {
        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType) {
            throw new DataAccessResourceFailureException("connection refused");
        }
    }

    @Test
    void disabledCheckDoesNotTouchTheDatabase() {
        assertDoesNotThrow(() -> new DatabaseStartupCheck(new ForbiddenJdbc(), false).run(null));
    }

    @Test
    void unreachableDatabaseDoesNotFailStartup() {
        assertDoesNotThrow(() -> new DatabaseStartupCheck(new DownJdbc(), true).run(null));
    }
}
