package no.unit.nva.useraccessservice.usercreation.person;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.API_DOMAIN;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.ENVIRONMENT;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.paths.UriWrapper;
import org.zalando.problem.Problem;

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

    // 1xxx - General server-side errors (application-level issues)

    /**
     * 1000: Generic error.
     * Used for unspecified or unhandled errors that don't fit other categories.
     */
    public static final ServiceErrorCode GENERIC_ERROR = new ServiceErrorCode(1000, HTTP_INTERNAL_ERROR);

    /**
     * 1001: Missing required fields validation error.
     * Used when data is missing required fields like ID, firstname, or surname.
     */
    public static final ServiceErrorCode MISSING_REQUIRED_FIELDS = new ServiceErrorCode(1001, HTTP_BAD_REQUEST);

    /**
     * 1002: Service unavailable due to infrastructure issues.
     * Used when the service cannot connect to external dependencies due to network problems,
     * timeouts, connection refused, or other infrastructure-level failures.
     */
    public static final ServiceErrorCode SERVICE_UNAVAILABLE = new ServiceErrorCode(1002, HTTP_GATEWAY_TIMEOUT);

    /**
     * 1003: Missing National Identity Number (NIN).
     * Used when a required National Identity Number is missing from user attributes.
     */
    public static final ServiceErrorCode MISSING_NIN = new ServiceErrorCode(1003, HTTP_BAD_REQUEST);

    // 2xxx - Resource-related errors (already exists, not found, etc.)

    /**
     * 2001: Person not found.
     * Used when a requested person cannot be found in the registry.
     */
    public static final ServiceErrorCode PERSON_NOT_FOUND = new ServiceErrorCode(2001, HTTP_NOT_FOUND);

    /**
     * 2002: Person already exists.
     * Used when attempting to create a person that already exists in the registry.
     */
    public static final ServiceErrorCode PERSON_ALREADY_EXISTS = new ServiceErrorCode(2002, HTTP_CONFLICT);

    // 3xxx - Upstream service errors (integration or API failures)

    /**
     * 3001: Upstream internal server error.
     * Used when the upstream person registry service responds with a 500+ error status.
     */
    public static final ServiceErrorCode UPSTREAM_INTERNAL_ERROR = new ServiceErrorCode(3001, HTTP_BAD_GATEWAY);

    /**
     * 3002: Upstream response parsing error.
     * Used when the response from the upstream person registry service cannot be parsed
     * (e.g., invalid JSON format or unexpected structure).
     */
    public static final ServiceErrorCode UPSTREAM_PARSING_ERROR = new ServiceErrorCode(3002, HTTP_BAD_GATEWAY);

    /**
     * 3003: Person creation failed.
     * Used when person creation fails in the upstream registry for reasons other than
     * the person already existing.
     */
    public static final ServiceErrorCode PERSON_CREATION_FAILED = new ServiceErrorCode(3003, HTTP_BAD_GATEWAY);

    private IdentityServiceErrorCodes() {
        // NO-OP
    }

    /**
     * Formats an error message as an RFC 9457 Problem Details JSON string.
     *
     * @param errorCode The ServiceErrorCode constant
     * @param message The error message
     * @return JSON string representation of the Problem
     */
    public static String formatMessage(ServiceErrorCode errorCode, String message) {
        try {
            var problem = createProblem(errorCode, message);
            return JsonUtils.dtoObjectMapper.writeValueAsString(problem);
        } catch (JsonProcessingException e) {
            return errorCode.asTitle(message);
        }
    }

    private static Problem createProblem(ServiceErrorCode errorCode, String message) {
        return Problem.builder()
            .withType(ERROR_TYPE_BASE_URI.addChild(errorCode.getErrorCodeString()).getUri())
            .withTitle(errorCode.asTitle(message))
            .withStatus(errorCode.getStatus())
            .withDetail(message)
            .build();
    }
}