package app.bevoya.api.learning;

import java.time.Instant;
import java.util.UUID;

record LearningEntryResponse(
    UUID id,
    String title,
    String content,
    String sourceUrl,
    Integer confidence,
    Instant createdAt,
    Instant updatedAt) {}