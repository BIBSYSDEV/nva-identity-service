package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.MISSING_REQUIRED_FIELDS;

public class IdentityServiceMissingRequiredFieldsException extends IdentityServiceException {
    public IdentityServiceMissingRequiredFieldsException(String message) {
        super(MISSING_REQUIRED_FIELDS, message);
    }
}