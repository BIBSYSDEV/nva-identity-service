package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;

public class IdentityServiceMissingRequiredFieldsException extends IdentityServiceException {
    public IdentityServiceMissingRequiredFieldsException(String message) {
        super(IdentityServiceErrorCodes.MISSING_REQUIRED_FIELDS, message);
    }

    public int getStatusCode() {
        return HTTP_BAD_REQUEST;
    }
}