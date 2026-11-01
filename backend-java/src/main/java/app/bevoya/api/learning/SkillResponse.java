package app.bevoya.api.learning;

import java.time.Instant;
import java.util.UUID;

record SkillResponse(
    UUID id,
    String name,
    String category,
    String description,
    String level,
    String targetLevel,
    int evidenceCount,
    int learningEntryCount,
    Instant lastPracticedAt,
    Instant nextRevisionAt,
    Instant createdAt,
    Instant updatedAt) {
}