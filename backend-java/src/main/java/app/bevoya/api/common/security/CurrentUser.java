package app.bevoya.api.common.security;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** The signed-in person, from the verified token only. Never build this from a request body or path. */
public record CurrentUser(UUID authUserId) {

  static CurrentUser from(Jwt jwt) {
    return new CurrentUser(UUID.fromString(jwt.getSubject()));
  }
}