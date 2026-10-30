package app.bevoya.api.profiles;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.ZoneId;

public class TimeZoneValidator implements ConstraintValidator<ValidTimeZone, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null || ZoneId.getAvailableZoneIds().contains(value);
  }
}