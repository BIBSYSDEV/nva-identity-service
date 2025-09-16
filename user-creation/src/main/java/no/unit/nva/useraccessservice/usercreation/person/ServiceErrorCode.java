package no.unit.nva.useraccessservice.usercreation.person;

import java.io.Serializable;
import org.zalando.problem.Status;

public record ServiceErrorCode(int serviceErrorCode, int httpStatusCode) implements Serializable {
    private static final String ERROR_PREFIX = "IdentityService-%d";
    
    public String asTitle(String message) {
        return ERROR_PREFIX.formatted(serviceErrorCode) + ": " + message;
    }
    
    public String getErrorCodeString() {
        return String.valueOf(serviceErrorCode);
    }
    
    public Status getStatus() {
        return Status.valueOf(httpStatusCode);
    }
}