package no.unit.nva.customer.testing;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import nva.commons.core.JacocoGenerated;

import java.util.Map;

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
            HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
            HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
    }

    /**
     * Successful response headers for testing.
     * @return headers
     */
    public static  Map<String,String> getResponseHeaders() {
        return Map.of(
                HttpHeaders.CONTENT_TYPE,  MediaType.JSON_UTF_8.toString(),
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD
        );
    }

    /**
     * Failing response headers for testing.
     * @return headers
     */
    public static  Map<String,String> getErrorResponseHeaders() {
        return Map.of(
                HttpHeaders.CONTENT_TYPE, APPLICATION_PROBLEM_JSON,
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD
        );
    }

}
