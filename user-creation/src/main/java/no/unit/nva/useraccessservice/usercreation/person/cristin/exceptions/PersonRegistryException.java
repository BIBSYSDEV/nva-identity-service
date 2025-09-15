package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

public class PersonRegistryException extends RuntimeException {

    private final String errorCode;

    public PersonRegistryException(String errorCode, String message) {
        super(PersonRegistryErrorCodes.formatMessage(errorCode, message));
        this.errorCode = errorCode;
    }

    public PersonRegistryException(String errorCode, String message, Throwable cause) {
        super(PersonRegistryErrorCodes.formatMessage(errorCode, message), cause);
        this.errorCode = errorCode;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Uses a generic error code.
     */
    public PersonRegistryException(String message) {
        super(message);
        this.errorCode = null;
    }

    /**
     * Gets the error code associated with this exception.
     * 
     * @return The error code, or null for legacy exceptions
     */
    public String getErrorCode() {
        return errorCode;
    }
}
