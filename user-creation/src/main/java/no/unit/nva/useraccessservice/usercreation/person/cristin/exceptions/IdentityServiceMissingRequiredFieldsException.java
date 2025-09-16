package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.MISSING_REQUIRED_FIELDS;

public class IdentityServiceMissingRequiredFieldsException extends IdentityServiceException {
    public IdentityServiceMissingRequiredFieldsException(String message) {
        super(MISSING_REQUIRED_FIELDS, message);
    }

    public int getStatusCode() {
        return HTTP_BAD_REQUEST;
    }
}