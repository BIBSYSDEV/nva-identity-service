package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;

public class IdentityServiceAlreadyExistsException extends IdentityServiceException {
    public IdentityServiceAlreadyExistsException(String message) {
        super(IdentityServiceErrorCodes.PERSON_ALREADY_EXISTS, message);
    }

    public IdentityServiceAlreadyExistsException(String message, Throwable cause) {
        super(IdentityServiceErrorCodes.PERSON_ALREADY_EXISTS, message, cause);
    }
    
    public int getStatusCode() {
        return HTTP_CONFLICT;
    }
}