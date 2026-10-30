package app.bevoya.api.profiles;

import java.time.Instant;
import java.util.UUID;

public record ConsentResponse(
    UUID id, String consentType, boolean granted, String policyVersion, Instant recordedAt) {
}