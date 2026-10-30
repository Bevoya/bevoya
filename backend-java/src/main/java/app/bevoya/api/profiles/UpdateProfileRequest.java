package app.bevoya.api.profiles;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH semantics: a missing or null field is left unchanged. There is no userId. */
public record UpdateProfileRequest(
    @Size(max = 80) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String displayName,
    @Pattern(regexp = "student|young_professional|working_adult|creator|other") String userType,
    @ValidTimeZone String timeZone,
    @Min(0) @Max(1440) Integer availableMinutesPerDay) {
}