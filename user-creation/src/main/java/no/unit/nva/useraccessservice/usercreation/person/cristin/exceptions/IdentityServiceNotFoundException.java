package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.PERSON_NOT_FOUND;

public class IdentityServiceNotFoundException extends IdentityServiceException {
    public IdentityServiceNotFoundException(String message) {
        super(PERSON_NOT_FOUND, message);
    }
}