package app.bevoya.api.planning;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record CreateTaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotNull @Pattern(regexp = TaskCategory.PATTERN) String category,
    @Min(1) @Max(3) Integer priority,
    @Min(1) @Max(1440) Integer estimatedMinutes,
    LocalDate plannedFor,
    Instant dueAt,
    @Pattern(regexp = TaskStatus.PATTERN) String status,
    @Size(max = 500) String whyItMatters,
    UUID goalId,
    UUID milestoneId) {}