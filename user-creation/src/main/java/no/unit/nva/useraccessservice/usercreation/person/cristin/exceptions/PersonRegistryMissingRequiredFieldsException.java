package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;

public class PersonRegistryMissingRequiredFieldsException extends PersonRegistryException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_BAD_REQUEST;

    public PersonRegistryMissingRequiredFieldsException(String message) {
        super(PersonRegistryErrorCodes.MISSING_REQUIRED_FIELDS, message);
    }

    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
}