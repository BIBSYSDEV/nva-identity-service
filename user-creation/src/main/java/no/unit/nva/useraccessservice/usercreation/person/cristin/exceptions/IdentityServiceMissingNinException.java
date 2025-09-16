package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.MISSING_NIN;

/**
 * Exception thrown when a required National Identity Number (NIN) is missing from user attributes.
 * This typically occurs during user authentication or identification processes.
 */
public class IdentityServiceMissingNinException extends IdentityServiceException {

    private static final String DEFAULT_MESSAGE = "Missing National Identity Number in user attributes";

    public IdentityServiceMissingNinException() {
        super(MISSING_NIN, DEFAULT_MESSAGE);
    }
    
    public IdentityServiceMissingNinException(String message) {
        super(MISSING_NIN, message);
    }

    public int getStatusCode() {
        return HTTP_BAD_REQUEST;
    }
}