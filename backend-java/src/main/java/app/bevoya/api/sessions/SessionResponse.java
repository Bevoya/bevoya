package app.bevoya.api.sessions;

import java.time.Instant;
import java.util.UUID;

record SessionResponse(
    UUID id,
    UUID taskId,
    String activityType,
    String topic,
    Instant startedAt,
    Instant endedAt,
    Integer actualMinutes,
    Integer focusScore,
    String output,
    String notes,
    Instant createdAt) {}