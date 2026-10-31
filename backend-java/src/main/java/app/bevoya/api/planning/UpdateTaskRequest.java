package app.bevoya.api.planning;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** PATCH: a null field means "leave as it is". Clearing a field is not supported yet. */
record UpdateTaskRequest(
    @Size(max = 200) @Pattern(regexp = ".*\\S.*") String title,
    @Size(max = 2000) String description,
    @Pattern(regexp = TaskCategory.PATTERN) String category,
    @Min(1) @Max(3) Integer priority,
    @Min(1) @Max(1440) Integer estimatedMinutes,
    LocalDate plannedFor,
    Instant dueAt,
    @Pattern(regexp = TaskStatus.PATTERN) String status,
    @Size(max = 500) String whyItMatters,
    UUID goalId,
    UUID milestoneId) {}