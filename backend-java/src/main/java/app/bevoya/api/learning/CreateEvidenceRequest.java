package app.bevoya.api.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

record CreateEvidenceRequest(
    @NotBlank @Pattern(regexp = EvidenceVocabulary.TYPE_PATTERN) String type,
    @NotBlank @Pattern(regexp = EvidenceVocabulary.LEVEL_PATTERN) String activityLevel,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 10000) String content,
    @Size(max = 2000) @Pattern(regexp = EvidenceVocabulary.HTTPS_PATTERN) String externalUrl,
    UUID sessionId) {}