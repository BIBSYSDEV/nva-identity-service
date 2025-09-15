package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;

public class IdentityServiceException extends RuntimeException {

    private final String errorCode;

    public IdentityServiceException(String errorCode, String message) {
        super(IdentityServiceErrorCodes.formatSimpleMessage(errorCode, message));
        this.errorCode = errorCode;
    }

    public IdentityServiceException(String errorCode, String message, Throwable cause) {
        super(IdentityServiceErrorCodes.formatSimpleMessage(errorCode, message), cause);
        this.errorCode = errorCode;
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