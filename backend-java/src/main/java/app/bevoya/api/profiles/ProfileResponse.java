package app.bevoya.api.profiles;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
    UUID authUserId,
    String displayName,
    String userType,
    String timeZone,
    Integer availableMinutesPerDay,
    Instant createdAt,
    Instant updatedAt) {
}