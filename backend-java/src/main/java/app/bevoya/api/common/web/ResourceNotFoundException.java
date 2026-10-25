package app.bevoya.api.common.web;

/** The record does not exist or is not yours. The two cases must be indistinguishable. */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException() {
    super("not found", null, false, false);
  }
}