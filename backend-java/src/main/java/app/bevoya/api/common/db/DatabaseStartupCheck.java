package app.bevoya.api.common.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs two trivial queries at startup and logs which database role the app
 * connected as and how long they took. It never fails the start: /health must
 * stay up even when the database is unreachable.
 *
 * Only infrastructure facts are logged here (role name, timings, the driver's
 * connection error). No user data is involved.
 */
@Component
class DatabaseStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupCheck.class);

    private final JdbcTemplate jdbc;
    private final boolean enabled;

    DatabaseStartupCheck(JdbcTemplate jdbc,
                         @Value("${bevoya.db.startup-check.enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            long start = System.nanoTime();
            String role = jdbc.queryForObject("select current_user", String.class);
            long firstMs = (System.nanoTime() - start) / 1_000_000;

            start = System.nanoTime();
            jdbc.queryForObject("select 1", Integer.class);
            long secondMs = (System.nanoTime() - start) / 1_000_000;

            log.info("Database reachable as {} (first query {} ms including connect, second query {} ms)",
                    role, firstMs, secondMs);
        } catch (Exception ex) {
            log.warn("Database not reachable at startup: {}",
                    NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
        }
    }
}
