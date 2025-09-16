package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.PERSON_ALREADY_EXISTS;

public class IdentityServiceAlreadyExistsException extends IdentityServiceException {
    public IdentityServiceAlreadyExistsException(String message) {
        super(PERSON_ALREADY_EXISTS, message);
    }

    public IdentityServiceAlreadyExistsException(String message, Throwable cause) {
        super(PERSON_ALREADY_EXISTS, message, cause);
    }
}