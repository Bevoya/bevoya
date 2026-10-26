package app.bevoya.api.common.security;

/** A valid token whose account no longer exists in auth.users. */
public class UnknownAccountException extends RuntimeException {

  public UnknownAccountException() {
    super("unknown account", null, false, false);
  }
}