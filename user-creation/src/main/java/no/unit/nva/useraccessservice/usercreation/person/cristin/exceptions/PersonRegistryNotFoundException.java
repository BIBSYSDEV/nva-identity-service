package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;

public class PersonRegistryNotFoundException extends PersonRegistryException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_NOT_FOUND;
    
    public PersonRegistryNotFoundException(String message) {
        super(PersonRegistryErrorCodes.PERSON_NOT_FOUND, message);
    }

    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}