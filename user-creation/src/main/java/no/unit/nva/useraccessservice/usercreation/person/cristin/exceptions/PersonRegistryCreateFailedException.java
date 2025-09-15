package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;

public class PersonRegistryCreateFailedException extends PersonRegistryException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_BAD_GATEWAY;

    public PersonRegistryCreateFailedException(String message, Throwable cause) {
        super(PersonRegistryErrorCodes.PERSON_CREATION_FAILED, message, cause);
    }
    
    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}