package app.bevoya.api.goals;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** PUT replaces everything. A null description or targetDate clears it. */
public record ReplaceGoalRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotNull @Pattern(regexp = Goal.STATUS_PATTERN) String status,
    LocalDate targetDate) {
}