package no.unit.nva.customer.testing;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.STRICT_TRANSPORT_SECURITY;
import static com.google.common.net.HttpHeaders.X_CONTENT_TYPE_OPTIONS;
import static com.google.common.net.MediaType.JSON_UTF_8;
import java.util.Map;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class TestHeaders {

    public static String WILDCARD = "*";

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
            X_CONTENT_TYPE_OPTIONS, "nosniff"
        );
    }
}
