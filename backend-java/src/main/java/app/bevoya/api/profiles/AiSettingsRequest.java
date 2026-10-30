package app.bevoya.api.profiles;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AiSettingsRequest(
    @NotNull Boolean aiMemoryEnabled,
    @NotNull Boolean journalRetrievalEnabled,
    @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,40}") String policyVersion) {

  /** Journal retrieval is a stricter permission than AI memory, so it cannot be on alone. */
  @AssertTrue(message = "journalRetrievalEnabled requires aiMemoryEnabled")
  public boolean isJournalRetrievalAllowed() {
    return !Boolean.TRUE.equals(journalRetrievalEnabled) || Boolean.TRUE.equals(aiMemoryEnabled);
  }
}