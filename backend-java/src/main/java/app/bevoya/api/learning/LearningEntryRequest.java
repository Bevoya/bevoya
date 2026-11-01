package app.bevoya.api.learning;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

record LearningEntryRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 20000) String content,
    @Size(max = 2000) @Pattern(regexp = EvidenceVocabulary.HTTPS_PATTERN) String sourceUrl,
    @Min(1) @Max(5) Integer confidence,
    UUID skillId) {}