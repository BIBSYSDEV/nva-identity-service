package no.unit.nva.customer.testing;

import nva.commons.core.JacocoGenerated;

import java.util.Map;

import static org.apache.hc.core5.http.HttpHeaders.ACCEPT;
import static org.apache.hc.core5.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.hc.core5.http.HttpHeaders.VARY;
import static nva.commons.apigateway.MediaType.JSON_UTF_8;

public class TestHeaders {

    private static final String WILDCARD = "*";
    private static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    /**
     * Request headers for testing.
     *
     * @return headers
     */
    public static Map<String, String> getRequestHeaders() {
        return Map.of(
            CONTENT_TYPE, JSON_UTF_8.toString(),
            ACCEPT, JSON_UTF_8.toString());
    }

    /**
     * Successful response headers for testing.
     *
     * @return headers
     */
    public static Map<String, String> getResponseHeaders() {
        return Map.of(
            CONTENT_TYPE, JSON_UTF_8.toString(),
            ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD,
            STRICT_TRANSPORT_SECURITY, "max-age=63072000; includeSubDomains; preload",
            X_CONTENT_TYPE_OPTIONS, "nosniff",
            VARY, "Origin, Accept"
        );
    }
}
