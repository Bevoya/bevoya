package app.bevoya.api.goals;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MilestoneResponse(
    UUID id,
    UUID goalId,
    String title,
    String description,
    String status,
    LocalDate targetDate,
    int position,
    Instant createdAt,
    Instant updatedAt) {
}