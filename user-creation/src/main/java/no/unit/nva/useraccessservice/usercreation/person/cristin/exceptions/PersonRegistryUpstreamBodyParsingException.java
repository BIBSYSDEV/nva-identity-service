package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;

public class PersonRegistryUpstreamBodyParsingException extends PersonRegistryException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_BAD_GATEWAY;

    public PersonRegistryUpstreamBodyParsingException(String message, Throwable cause) {
        super(PersonRegistryErrorCodes.UPSTREAM_PARSING_ERROR, message, cause);
    }
    
    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}