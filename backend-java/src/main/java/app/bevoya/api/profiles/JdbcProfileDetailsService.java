package app.bevoya.api.profiles;

import app.bevoya.api.identity.ProfileService;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
class JdbcProfileDetailsService implements ProfileDetailsService {

  private static final String COLUMNS =
      "id, display_name, user_type, time_zone, available_minutes_per_day, created_at, updated_at";

  private static final RowMapper<ProfileResponse> MAPPER = (rs, n) -> new ProfileResponse(
      rs.getObject("id", UUID.class),
      rs.getString("display_name"),
      rs.getString("user_type"),
      rs.getString("time_zone"),
      rs.getObject("available_minutes_per_day", Integer.class),
      rs.getObject("created_at", OffsetDateTime.class).toInstant(),
      rs.getObject("updated_at", OffsetDateTime.class).toInstant());

  private final JdbcClient jdbc;
  private final ProfileService profiles;

  JdbcProfileDetailsService(JdbcClient jdbc, ProfileService profiles) {
    this.jdbc = jdbc;
    this.profiles = profiles;
  }

  @Override
  public ProfileResponse get(UUID userId) {
    profiles.ensureExists(userId);
    return jdbc.sql("select " + COLUMNS + " from public.profiles where id = :id")
        .param("id", userId)
        .query(MAPPER)
        .single();
  }

  @Override
  public ProfileResponse update(UUID userId, UpdateProfileRequest request) {
    profiles.ensureExists(userId);
    String name = request.displayName() == null ? null : request.displayName().strip();
    return jdbc.sql("update public.profiles set "
            + "display_name = coalesce(:name, display_name), "
            + "user_type = coalesce(:type, user_type), "
            + "time_zone = coalesce(:zone, time_zone), "
            + "available_minutes_per_day = coalesce(:minutes, available_minutes_per_day), "
            + "updated_at = now() "
            + "where id = :id returning " + COLUMNS)
        .param("name", name, Types.VARCHAR)
        .param("type", request.userType(), Types.VARCHAR)
        .param("zone", request.timeZone(), Types.VARCHAR)
        .param("minutes", request.availableMinutesPerDay(), Types.INTEGER)
        .param("id", userId)
        .query(MAPPER)
        .single();
  }
}