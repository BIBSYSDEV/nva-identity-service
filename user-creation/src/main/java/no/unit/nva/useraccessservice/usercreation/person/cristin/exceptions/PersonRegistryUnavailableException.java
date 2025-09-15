package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.HttpURLConnection;

/**
 * Exception thrown when the Person Registry service is unavailable due to network issues,
 * connection problems, timeouts, or other infrastructure-related failures.
 * This is different from PersonRegistryUpstreamInternalServerErrorException which indicates
 * that the service responded with a 500+ error.
 */
public class PersonRegistryUnavailableException extends PersonRegistryException {

    public static final int HTTP_STATUS_CODE = HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
    private static final String DEFAULT_MESSAGE = "Person Registry service is currently unavailable";
    
    public PersonRegistryUnavailableException(String message) {
        super(PersonRegistryErrorCodes.SERVICE_UNAVAILABLE, message);
    }

    public PersonRegistryUnavailableException(String message, Throwable cause) {
        super(PersonRegistryErrorCodes.SERVICE_UNAVAILABLE, message, cause);
    }
    
    public PersonRegistryUnavailableException(Throwable cause) {
        super(PersonRegistryErrorCodes.SERVICE_UNAVAILABLE, DEFAULT_MESSAGE, cause);
    }
    
    public int getStatusCode() {
        return HTTP_STATUS_CODE;
    }
    
    /**
     * Creates an exception with a detailed message about the network failure.
     * 
     * @param uri The URI that was being accessed
     * @param cause The underlying cause of the failure
     * @return A new PersonRegistryUnavailableException with detailed message
     */
    public static PersonRegistryUnavailableException withDetails(String uri, Throwable cause) {
        var message = String.format("Unable to connect to Person Registry at %s: %s", 
                                    uri, 
                                    cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName());
        return new PersonRegistryUnavailableException(message, cause);
    }
}