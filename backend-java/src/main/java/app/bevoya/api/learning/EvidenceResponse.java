package app.bevoya.api.learning;

import java.time.Instant;
import java.util.UUID;

record EvidenceResponse(
    UUID id,
    UUID sessionId,
    String type,
    String activityLevel,
    String title,
    String content,
    String externalUrl,
    Instant createdAt) {}