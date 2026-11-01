package app.bevoya.api.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

record SkillRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 50) String category,
    @Size(max = 1000) String description,
    @Pattern(regexp = "discovered|studying|explained|practiced|built|tested|applied") String targetLevel,
    Instant nextRevisionAt) {
}