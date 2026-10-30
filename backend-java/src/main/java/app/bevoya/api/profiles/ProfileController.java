package app.bevoya.api.profiles;

import app.bevoya.api.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProfileController {

  private final ProfileDetailsService profiles;

  ProfileController(ProfileDetailsService profiles) {
    this.profiles = profiles;
  }

  /** Who the verified token says you are, plus your profile. Creates the profile on first use. */
  @GetMapping("/me")
  ProfileResponse me(CurrentUser user) {
    return profiles.get(user.authUserId());
  }

  @PatchMapping("/me")
  ProfileResponse update(CurrentUser user, @Valid @RequestBody UpdateProfileRequest request) {
    return profiles.update(user.authUserId(), request);
  }
}