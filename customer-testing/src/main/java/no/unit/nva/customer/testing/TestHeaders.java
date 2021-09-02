package no.unit.nva.customer.testing;

import nva.commons.apigateway.ContentTypes;
import nva.commons.core.JacocoGenerated;

import java.util.Map;

import static nva.commons.apigateway.ApiGatewayHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static nva.commons.apigateway.ApiGatewayHandler.CONTENT_TYPE;
import static nva.commons.apigateway.HttpHeaders.ACCEPT;

@JacocoGenerated
public class TestHeaders {

    public static String APPLICATION_PROBLEM_JSON = "application/problem+json";
    public static String WILDCARD = "*";

    /**
     * Request headers for testing.
     * @return headers
     */
    public static Map<String,String> getRequestHeaders() {
        return Map.of(
            CONTENT_TYPE, ContentTypes.APPLICATION_JSON,
            ACCEPT, ContentTypes.APPLICATION_JSON);
    }

    /**
     * Successful response headers for testing.
     * @return headers
     */
    public static  Map<String,String> getResponseHeaders() {
        return Map.of(
                CONTENT_TYPE,  ContentTypes.APPLICATION_JSON,
                ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD
        );
    }

    /**
     * Failing response headers for testing.
     * @return headers
     */
    public static  Map<String,String> getErrorResponseHeaders() {
        return Map.of(
                CONTENT_TYPE, APPLICATION_PROBLEM_JSON,
                ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD
        );
    }

}
