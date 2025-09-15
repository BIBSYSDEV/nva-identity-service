package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;
import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;

public class IdentityServiceNotFoundException extends IdentityServiceException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_NOT_FOUND;
    
    public IdentityServiceNotFoundException(String message) {
        super(IdentityServiceErrorCodes.PERSON_NOT_FOUND, message);
    }

    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}