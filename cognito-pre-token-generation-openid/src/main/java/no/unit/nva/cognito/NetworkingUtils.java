package no.unit.nva.cognito;

import static no.unit.nva.cognito.EnvironmentVariables.API_DOMAIN;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import nva.commons.core.paths.UriWrapper;

public final class NetworkingUtils {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String APPLICATION_JSON = "application/json";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String JWT_TOKEN_FIELD = "access_token";
    public static final URI CRISTIN_HOST = UriWrapper.fromHost(API_DOMAIN).addChild("cristin-internal").getUri();
    public static final String CRISTIN_PATH_FOR_GETTING_USER_BY_NIN ="person/identityNumber";

    // This should be equal to the "ClientName" field of in the UserPoolClient entry for the Backend client n the
    // template file
    public static final String BACKEND_USER_POOL_CLIENT_NAME = "BackendApplicationClient";
    public static final Map<String, String> GRANT_TYPE_CLIENT_CREDENTIALS = Map.of("grant_type", "client_credentials");


    private NetworkingUtils() {

    }

    public static URI standardOauth2TokenEndpoint(URI cognitoHost) {
        return new UriWrapper(cognitoHost).addChild("oauth2").addChild("token").getUri();
    }

    public static String formatBasicAuthenticationHeader(String clientId, String clientSecret) {
        return attempt(() -> String.format("%s:%s", clientId, clientSecret))
            .map(str -> Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8)))
            .map(credentials -> "Basic " + credentials)
            .orElseThrow();
    }
}
