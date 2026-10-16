package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.MISSING_IDENTITY_LINK;

/**
 * Exception thrown when a login carries no National Identity Number and nothing stored links the
 * Cognito user to a person, so the user cannot be resolved.
 */
public class IdentityServiceMissingLinkException extends IdentityServiceException {

  private static final String DEFAULT_MESSAGE = "No stored link from the Cognito user to a person";

  public IdentityServiceMissingLinkException() {
    super(MISSING_IDENTITY_LINK, DEFAULT_MESSAGE);
  }

  public IdentityServiceMissingLinkException(String message) {
    super(MISSING_IDENTITY_LINK, message);
  }
}
