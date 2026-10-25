package app.bevoya.api.identity;

import java.util.UUID;

/** Makes sure the signed-in person has a profile row. */
public interface ProfileService {

  /** Idempotent. profiles.id is the auth user id (V3), so there is nothing to look up. */
  void ensureExists(UUID authUserId);
}