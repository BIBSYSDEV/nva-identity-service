package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;
import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;

public class IdentityServiceAlreadyExistsException extends IdentityServiceException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_CONFLICT;
    
    public IdentityServiceAlreadyExistsException(String message) {
        super(IdentityServiceErrorCodes.PERSON_ALREADY_EXISTS, message);
    }

    public IdentityServiceAlreadyExistsException(String message, Throwable cause) {
        super(IdentityServiceErrorCodes.PERSON_ALREADY_EXISTS, message, cause);
    }
    
    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}