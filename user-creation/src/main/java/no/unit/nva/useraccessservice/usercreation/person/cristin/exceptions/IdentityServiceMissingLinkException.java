package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.MISSING_IDENTITY_LINK;

/**
 * Exception thrown when nothing usable links the Cognito user to a person: a password user without
 * a valid person identifier, or a stored link whose person row is missing.
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
