package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;
import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;

public class IdentityServiceUpstreamBodyParsingException extends IdentityServiceException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_BAD_GATEWAY;

    public IdentityServiceUpstreamBodyParsingException(String message, Throwable cause) {
        super(IdentityServiceErrorCodes.UPSTREAM_PARSING_ERROR, message, cause);
    }
    
    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}