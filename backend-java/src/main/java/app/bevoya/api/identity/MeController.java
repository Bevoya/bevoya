package app.bevoya.api.identity;

import app.bevoya.api.common.security.CurrentUser;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

  /** Who the verified token says you are. No database yet; the profile lookup arrives with the goals branch. */
  @GetMapping("/me")
  Map<String, String> me(CurrentUser user) {
    return Map.of("authUserId", user.authUserId().toString());
  }
}