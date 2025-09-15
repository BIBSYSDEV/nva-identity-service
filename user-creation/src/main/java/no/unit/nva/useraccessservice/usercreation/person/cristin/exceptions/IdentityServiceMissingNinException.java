package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;
import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;

/**
 * Exception thrown when a required National Identity Number (NIN) is missing from user attributes.
 * This typically occurs during user authentication or identification processes.
 */
public class IdentityServiceMissingNinException extends IdentityServiceException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_BAD_REQUEST;
    private static final String DEFAULT_MESSAGE = "Missing National Identity Number in user attributes";

    public IdentityServiceMissingNinException() {
        super(IdentityServiceErrorCodes.MISSING_NIN, DEFAULT_MESSAGE);
    }
    
    public IdentityServiceMissingNinException(String message) {
        super(IdentityServiceErrorCodes.MISSING_NIN, message);
    }

    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}