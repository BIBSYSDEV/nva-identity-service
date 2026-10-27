package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.LOGIN_NOT_COMPLETED;

/**
 * Thrown by the inbound federation trigger when the person behind a login could not be resolved,
 * for any reason other than a missing National Identity Number. The message reaches the client, so
 * it stays the same whatever the reason; the reason is logged.
 */
public class IdentityServiceLoginNotCompletedException extends IdentityServiceException {

  private static final String DEFAULT_MESSAGE = "Login could not be completed";

  public IdentityServiceLoginNotCompletedException() {
    super(LOGIN_NOT_COMPLETED, DEFAULT_MESSAGE);
  }

  public IdentityServiceLoginNotCompletedException(Throwable cause) {
    super(LOGIN_NOT_COMPLETED, DEFAULT_MESSAGE, cause);
  }
}
