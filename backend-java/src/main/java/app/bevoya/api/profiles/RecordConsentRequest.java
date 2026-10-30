package app.bevoya.api.profiles;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Consents are append-only: withdrawing is a new row with granted=false. */
public record RecordConsentRequest(
    @NotNull @Pattern(regexp = "age_18_plus|terms|privacy_policy|ai_memory|journal_retrieval")
    String consentType,
    @NotNull Boolean granted,
    @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,40}") String policyVersion) {
}