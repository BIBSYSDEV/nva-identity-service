package no.unit.nva.useraccessservice.usercreation.person;

/**
 * Central registry of error codes for Identity Service exceptions.
 * 
 * <p>Error Code Structure:
 * - IdentityService-xxxx: All error codes are prefixed with "IdentityService-"
 * - 1xxx: General server-side errors (application-level issues, infrastructure problems)
 * - 2xxx: Resource-related errors (e.g., already exists, not found, etc.)
 * - 3xxx: Upstream service errors (e.g., API response failures, service-level issues)
 */
public final class IdentityServiceErrorCodes {

    // 1xxx - General server-side errors (application-level issues)
    
    /**
     * 1000: Generic error.
     * Used for unspecified or unhandled errors that don't fit other categories.
     */
    public static final String GENERIC_ERROR = "1000";
    
    /**
     * 1001: Missing required fields validation error.
     * Used when data is missing required fields like ID, firstname, or surname.
     */
    public static final String MISSING_REQUIRED_FIELDS = "1001";
    
    /**
     * 1002: Service unavailable due to infrastructure issues.
     * Used when the service cannot connect to external dependencies due to network problems,
     * timeouts, connection refused, or other infrastructure-level failures.
     */
    public static final String SERVICE_UNAVAILABLE = "1002";
    
    /**
     * 1003: Missing National Identity Number (NIN).
     * Used when a required National Identity Number is missing from user attributes.
     */
    public static final String MISSING_NIN = "1003";

    // 2xxx - Resource-related errors (already exists, not found, etc.)
    
    /**
     * 2001: Person not found.
     * Used when a requested person cannot be found in the registry.
     */
    public static final String PERSON_NOT_FOUND = "2001";

    /**
     * 2002: Person already exists.
     * Used when attempting to create a person that already exists in the registry.
     */
    public static final String PERSON_ALREADY_EXISTS = "2002";

    // 3xxx - Upstream service errors (integration or API failures)

    /**
     * 3001: Upstream internal server error.
     * Used when the upstream person registry service responds with a 500+ error status.
     */
    public static final String UPSTREAM_INTERNAL_ERROR = "3001";

    /**
     * 3002: Upstream response parsing error.
     * Used when the response from the upstream person registry service cannot be parsed
     * (e.g., invalid JSON format or unexpected structure).
     */
    public static final String UPSTREAM_PARSING_ERROR = "3002";

    /**
     * 3003: Person creation failed.
     * Used when person creation fails in the upstream registry for reasons other than
     * the person already existing.
     */
    public static final String PERSON_CREATION_FAILED = "3003";

    private IdentityServiceErrorCodes() {
        // Utility class
    }

    /**
     * Formats an error message with the IdentityService error code prefix.
     * 
     * @param errorCode The error code constant (without IdentityService- prefix)
     * @param message The error message
     * @return Formatted message with full IdentityService-xxxx: prefix
     */
    public static String formatMessage(String errorCode, String message) {
        return "IdentityService-" + errorCode + ": " + message;
    }

    /**
     * Formats a PersonRegistry-specific error message for backward compatibility.
     * 
     * @param errorCode The error code constant (without prefix)
     * @param message The error message
     * @return Formatted message with PersonRegistry-xxxx: prefix for backward compatibility
     */
    public static String formatPersonRegistryMessage(String errorCode, String message) {
        return "PersonRegistry-" + errorCode + ": " + message;
    }
}