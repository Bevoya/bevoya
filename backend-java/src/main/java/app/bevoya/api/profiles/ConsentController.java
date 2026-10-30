package app.bevoya.api.profiles;

import app.bevoya.api.common.idempotency.IdempotencyStore;
import app.bevoya.api.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
class ConsentController {

  private final ConsentService consents;

  ConsentController(ConsentService consents) {
    this.consents = consents;
  }

  @GetMapping("/consents")
  List<ConsentResponse> current(CurrentUser user) {
    return consents.current(user.authUserId());
  }

  @PostMapping("/consents")
  @ResponseStatus(HttpStatus.CREATED)
  ConsentResponse record(CurrentUser user,
      @RequestHeader(name = IdempotencyStore.HEADER, required = false)
      @Pattern(regexp = IdempotencyStore.KEY_PATTERN) String idempotencyKey,
      @Valid @RequestBody RecordConsentRequest request) {
    return consents.record(user.authUserId(), idempotencyKey, request);
  }

  @GetMapping("/ai-settings")
  AiSettingsResponse aiSettings(CurrentUser user) {
    return consents.aiSettings(user.authUserId());
  }

  @PutMapping("/ai-settings")
  AiSettingsResponse updateAiSettings(CurrentUser user, @Valid @RequestBody AiSettingsRequest request) {
    return consents.updateAiSettings(user.authUserId(), request);
  }
}