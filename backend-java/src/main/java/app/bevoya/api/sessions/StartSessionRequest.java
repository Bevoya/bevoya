package app.bevoya.api.sessions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

record StartSessionRequest(
    @NotBlank @Pattern(regexp = SessionActivity.PATTERN) String activityType,
    UUID taskId,
    @Size(max = 200) String topic) {}