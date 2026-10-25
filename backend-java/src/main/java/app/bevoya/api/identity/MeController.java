package app.bevoya.api.identity;

import app.bevoya.api.common.security.CurrentUser;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

  private final ProfileService profiles;

  MeController(ProfileService profiles) {
    this.profiles = profiles;
  }

  /** Who the verified token says you are. Also creates the profile row on first use. */
  @GetMapping("/me")
  Map<String, String> me(CurrentUser user) {
    profiles.ensureExists(user.authUserId());
    return Map.of("authUserId", user.authUserId().toString());
  }
}