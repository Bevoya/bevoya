package app.bevoya.api.goals;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** No userId, goalId or position here: the owner comes from the token, the goal from the path. */
public record CreateMilestoneRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @Pattern(regexp = MilestoneStatus.PATTERN) String status,
    LocalDate targetDate) {
}