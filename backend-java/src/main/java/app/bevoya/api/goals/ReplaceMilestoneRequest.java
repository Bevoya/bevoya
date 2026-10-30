package app.bevoya.api.goals;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** PUT replaces the content. A null description or targetDate clears it. Position is untouched. */
public record ReplaceMilestoneRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotNull @Pattern(regexp = MilestoneStatus.PATTERN) String status,
    LocalDate targetDate) {
}