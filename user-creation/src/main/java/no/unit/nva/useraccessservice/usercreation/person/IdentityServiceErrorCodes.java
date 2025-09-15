package no.unit.nva.useraccessservice.usercreation.person;

import static no.unit.nva.useraccessservice.constants.ServiceConstants.API_DOMAIN;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.ENVIRONMENT;
import static nva.commons.core.StringUtils.isEmpty;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.paths.UriWrapper;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

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
    public static final String BASE_PATH =  ENVIRONMENT.readEnv("BASE_PATH");

    private static final UriWrapper ERROR_TYPE_BASE_URI = UriWrapper.fromHost(API_DOMAIN)
                                                              .addChild(BASE_PATH)
                                                              .addChild("errors");
    private static final String ERROR_PREFIX = "IdentityService-";

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
     * Formats an error message as an RFC 7807 Problem Details JSON string.
     *
     * @param errorCode The error code constant (without IdentityService- prefix)
     * @param message The error message
     * @return JSON string representation of the Problem
     */
    public static String formatMessage(String errorCode, String message) {
        return formatMessage(errorCode, message, getStatusForErrorCode(errorCode));
    }

    /**
     * Formats an error message as an RFC 7807 Problem Details JSON string with custom status.
     *
     * @param errorCode The error code constant (without IdentityService- prefix)
     * @param message The error message
     * @param status The HTTP status
     * @return JSON string representation of the Problem
     */
    public static String formatMessage(String errorCode, String message, Status status) {
        try {
            var problem = createProblem(errorCode, message, status);
            return JsonUtils.dtoObjectMapper.writeValueAsString(problem);
        } catch (JsonProcessingException e) {
            // Fallback to a simple string format if JSON serialization fails
            return ERROR_PREFIX + errorCode + ": " + message;
        }
    }

    /**
     * Creates a Problem object for the given error code and message.
     *
     * @param errorCode The error code constant (without prefix)
     * @param message The error message
     * @return Problem object
     */
    public static Problem createProblem(String errorCode, String message) {
        return createProblem(errorCode, message, getStatusForErrorCode(errorCode));
    }

    /**
     * Creates a Problem object for the given error code, message, and status.
     *
     * @param errorCode The error code constant (without prefix)
     * @param message The error message
     * @param status The HTTP status
     * @return Problem object
     */
    private static Problem createProblem(String errorCode, String message, Status status) {
        return Problem.builder()
            .withType(ERROR_TYPE_BASE_URI.addChild(errorCode).getUri())
            .withTitle(ERROR_PREFIX + errorCode + ": " + message)
            .withStatus(status)
            .withDetail(message)
            .build();
    }

    /**
     * Determines the appropriate HTTP status for an error code.
     *
     * @param errorCode The error code
     * @return The corresponding HTTP status
     */
    private static Status getStatusForErrorCode(String errorCode) {
        if (isEmpty(errorCode)) {
            return Status.INTERNAL_SERVER_ERROR;
        }

        // Parse the first digit to determine the category
        char firstDigit = errorCode.charAt(0);

        return switch (firstDigit) {
            case '1' -> {
                // 1xxx - Server-side errors
                if (SERVICE_UNAVAILABLE.equals(errorCode)) {
                    yield Status.GATEWAY_TIMEOUT;
                } else if (MISSING_REQUIRED_FIELDS.equals(errorCode) || MISSING_NIN.equals(errorCode)) {
                    yield Status.BAD_REQUEST;
                }
                yield Status.INTERNAL_SERVER_ERROR;
            }
            case '2' -> {
                // 2xxx - Resource errors
                if (PERSON_NOT_FOUND.equals(errorCode)) {
                    yield Status.NOT_FOUND;
                } else if (PERSON_ALREADY_EXISTS.equals(errorCode)) {
                    yield Status.CONFLICT;
                }
                yield Status.NOT_FOUND;
            }
            case '3' -> 
                // 3xxx - Upstream errors
                Status.BAD_GATEWAY;
            default -> 
                Status.INTERNAL_SERVER_ERROR;
        };
    }
}