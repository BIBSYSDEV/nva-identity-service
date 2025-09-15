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

    public String getErrorCode() {
        return errorCode;
    }
}