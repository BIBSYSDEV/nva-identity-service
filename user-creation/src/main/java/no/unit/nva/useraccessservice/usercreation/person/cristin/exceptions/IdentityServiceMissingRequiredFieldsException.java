package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;
import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;

public class IdentityServiceMissingRequiredFieldsException extends IdentityServiceException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_BAD_REQUEST;

    public IdentityServiceMissingRequiredFieldsException(String message) {
        super(IdentityServiceErrorCodes.MISSING_REQUIRED_FIELDS, message);
    }

    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}