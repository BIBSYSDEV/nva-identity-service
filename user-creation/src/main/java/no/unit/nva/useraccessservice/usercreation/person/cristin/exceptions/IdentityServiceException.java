package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;

public class IdentityServiceException extends RuntimeException {

    private final String errorCode;

    public IdentityServiceException(String errorCode, String message) {
        super(IdentityServiceErrorCodes.formatMessage(errorCode, message));
        this.errorCode = errorCode;
    }

    public IdentityServiceException(String errorCode, String message, Throwable cause) {
        super(IdentityServiceErrorCodes.formatMessage(errorCode, message), cause);
        this.errorCode = errorCode;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Uses a generic error code.
     */
    public IdentityServiceException(String message) {
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