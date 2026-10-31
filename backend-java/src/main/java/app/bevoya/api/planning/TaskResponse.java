package app.bevoya.api.planning;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record TaskResponse(
    UUID id,
    UUID goalId,
    UUID milestoneId,
    String title,
    String description,
    String category,
    int priority,
    Integer estimatedMinutes,
    LocalDate plannedFor,
    Instant dueAt,
    String status,
    String whyItMatters,
    Instant completedAt,
    Instant createdAt,
    Instant updatedAt) {}