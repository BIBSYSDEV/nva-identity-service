package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;

public class PersonRegistryAlreadyExistsException extends PersonRegistryException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_CONFLICT;
    
    public PersonRegistryAlreadyExistsException(String message) {
        super(PersonRegistryErrorCodes.PERSON_ALREADY_EXISTS, message);
    }

    public PersonRegistryAlreadyExistsException(String message, Throwable cause) {
        super(PersonRegistryErrorCodes.PERSON_ALREADY_EXISTS, message, cause);
    }
    
    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}