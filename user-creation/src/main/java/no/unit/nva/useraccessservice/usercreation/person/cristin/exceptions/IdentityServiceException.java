package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;
import no.unit.nva.useraccessservice.usercreation.person.ServiceErrorCode;

public class IdentityServiceException extends RuntimeException {

    private final ServiceErrorCode errorCode;

    public IdentityServiceException(ServiceErrorCode errorCode, String message) {
        super(IdentityServiceErrorCodes.formatMessage(errorCode, message));
        this.errorCode = errorCode;
    }

    public IdentityServiceException(ServiceErrorCode errorCode, String message, Throwable cause) {
        super(IdentityServiceErrorCodes.formatMessage(errorCode, message), cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode.getErrorCodeString();
    }
    
    public int getStatusCode() {
        return errorCode.httpStatusCode();
    }
}