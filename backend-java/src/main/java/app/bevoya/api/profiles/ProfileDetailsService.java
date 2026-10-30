package app.bevoya.api.profiles;

import java.util.UUID;

/** Reads and updates the signed-in person's profile. An interface so no-database tests can stub it. */
public interface ProfileDetailsService {

  /** Creates the profile row on first use, then returns it. */
  ProfileResponse get(UUID userId);

  /** PATCH semantics: null fields are left unchanged. */
  ProfileResponse update(UUID userId, UpdateProfileRequest request);
}