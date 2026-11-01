package app.bevoya.api.reflections;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

record ReflectionRequest(
    LocalDate entryDate,
    @NotBlank @Size(max = 20000) String rawText,
    @Min(1) @Max(5) Integer mood,
    @Min(1) @Max(5) Integer energy) {
}