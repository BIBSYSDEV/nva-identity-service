package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;

public class PersonRegistryUpstreamInternalServerErrorException extends PersonRegistryException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_BAD_GATEWAY;
    
    public PersonRegistryUpstreamInternalServerErrorException(String message) {
        super(PersonRegistryErrorCodes.UPSTREAM_INTERNAL_ERROR, message);
    }

    public PersonRegistryUpstreamInternalServerErrorException(String message, Throwable cause) {
        super(PersonRegistryErrorCodes.UPSTREAM_INTERNAL_ERROR, message, cause);
    }
    
    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}