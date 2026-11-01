package app.bevoya.api.reflections;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record ReflectionResponse(
    UUID id,
    LocalDate entryDate,
    String rawText,
    Integer mood,
    Integer energy,
    Instant createdAt,
    Instant updatedAt) {
}