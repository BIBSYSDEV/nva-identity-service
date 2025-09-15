package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;
import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;

public class IdentityServiceUpstreamInternalServerErrorException extends IdentityServiceException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_BAD_GATEWAY;
    
    public IdentityServiceUpstreamInternalServerErrorException(String message) {
        super(IdentityServiceErrorCodes.UPSTREAM_INTERNAL_ERROR, message);
    }

    public IdentityServiceUpstreamInternalServerErrorException(String message, Throwable cause) {
        super(IdentityServiceErrorCodes.UPSTREAM_INTERNAL_ERROR, message, cause);
    }
    
    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}