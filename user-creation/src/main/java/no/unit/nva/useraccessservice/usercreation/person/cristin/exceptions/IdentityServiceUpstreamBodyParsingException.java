package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.UPSTREAM_PARSING_ERROR;

public class IdentityServiceUpstreamBodyParsingException extends IdentityServiceException {
    public IdentityServiceUpstreamBodyParsingException(String message, Throwable cause) {
        super(UPSTREAM_PARSING_ERROR, message, cause);
    }
    
    public int getStatusCode() {
        return HTTP_BAD_GATEWAY;
    }
}