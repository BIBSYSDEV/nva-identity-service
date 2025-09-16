package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.PERSON_CREATION_FAILED;

public class IdentityServiceCreateFailedException extends IdentityServiceException {
    public IdentityServiceCreateFailedException(String message, Throwable cause) {
        super(PERSON_CREATION_FAILED, message, cause);
    }
    
    public int getStatusCode() {
        return HTTP_BAD_GATEWAY;
    }
}