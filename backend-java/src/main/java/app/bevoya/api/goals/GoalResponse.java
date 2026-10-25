package app.bevoya.api.goals;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
    UUID id,
    String title,
    String description,
    String status,
    LocalDate targetDate,
    Instant createdAt,
    Instant updatedAt) {

  static GoalResponse from(Goal goal) {
    return new GoalResponse(goal.getId(), goal.getTitle(), goal.getDescription(), goal.getStatus(),
        goal.getTargetDate(), goal.getCreatedAt(), goal.getUpdatedAt());
  }
}