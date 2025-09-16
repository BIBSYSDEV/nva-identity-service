package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.SERVICE_UNAVAILABLE;

/**
 * Exception thrown when the Person Registry service is unavailable due to network issues,
 * connection problems, timeouts, or other infrastructure-related failures.
 * This is different from IdentityServiceUpstreamInternalServerErrorException which indicates
 * that the service responded with a 500+ error.
 */
public class IdentityServiceUnavailableException extends IdentityServiceException {
    private static final String DEFAULT_MESSAGE = "Person Registry service is currently unavailable";
    
    public IdentityServiceUnavailableException(String message) {
        super(SERVICE_UNAVAILABLE, message);
    }

    public IdentityServiceUnavailableException(String message, Throwable cause) {
        super(SERVICE_UNAVAILABLE, message, cause);
    }
    
    public IdentityServiceUnavailableException(Throwable cause) {
        super(SERVICE_UNAVAILABLE, DEFAULT_MESSAGE, cause);
    }
    
    public int getStatusCode() {
        return HTTP_GATEWAY_TIMEOUT;
    }
    
    /**
     * Creates an exception with a detailed message about the network failure.
     * 
     * @param uri The URI that was being accessed
     * @param cause The underlying cause of the failure
     * @return A new IdentityServiceUnavailableException with detailed message
     */
    public static IdentityServiceUnavailableException withDetails(String uri, Throwable cause) {
        var message = String.format("Unable to connect to Person Registry at %s: %s", 
                                    uri, 
                                    cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName());
        return new IdentityServiceUnavailableException(message, cause);
    }
}