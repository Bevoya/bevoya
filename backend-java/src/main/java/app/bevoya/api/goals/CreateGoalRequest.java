package app.bevoya.api.goals;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** There is deliberately no userId here. The owner comes from the token only. */
public record CreateGoalRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @Pattern(regexp = Goal.STATUS_PATTERN) String status,
    LocalDate targetDate) {
}