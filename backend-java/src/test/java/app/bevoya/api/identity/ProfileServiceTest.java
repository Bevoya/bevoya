package app.bevoya.api.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Needs Docker, so it is skipped on the dev laptop (ADR-0006) and runs in CI.
 * It connects as the container's admin user, so it checks the SQL and the foreign key,
 * not the bevoya_app grants or RLS.
 */
@Testcontainers(disabledWithoutDocker = true)
class ProfileServiceTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
      .withDatabaseName("bevoya")
      .withUsername("postgres")
      .withPassword("postgres")
      // The same stand-ins for Supabase that the ci-db workflow uses (anon, auth.users, ...).
      .withCopyFileToContainer(
          MountableFile.forHostPath("../infrastructure/db/ci-bootstrap.sql"),
          "/docker-entrypoint-initdb.d/00-ci-bootstrap.sql");

  static JdbcClient jdbc;
  static ProfileService service;

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
    var dataSource = new DriverManagerDataSource(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    jdbc = JdbcClient.create(dataSource);
    service = new JdbcProfileService(jdbc);
  }

  private static void createAuthUser(UUID id) {
    jdbc.sql("insert into auth.users (id) values (:id)").param("id", id).update();
  }

  private static long profileCount(UUID id) {
    return jdbc.sql("select count(*) from public.profiles where id = :id")
        .param("id", id).query(Long.class).single();
  }

  @Test
  void createsTheProfileOnFirstUse() {
    UUID id = UUID.randomUUID();
    createAuthUser(id);
    service.ensureExists(id);
    assertEquals(1, profileCount(id));
  }

  @Test
  void callingItAgainChangesNothing() {
    UUID id = UUID.randomUUID();
    createAuthUser(id);
    service.ensureExists(id);
    service.ensureExists(id);
    assertEquals(1, profileCount(id));
  }

  @Test
  void aTokenForAnAccountThatNoLongerExistsIsRefusedByTheDatabase() {
    UUID id = UUID.randomUUID(); // never inserted into auth.users
    assertThrows(DataIntegrityViolationException.class, () -> service.ensureExists(id));
    assertEquals(0, profileCount(id));
  }
}