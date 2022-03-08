package no.unit.nva.cognito;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.regions.Region;

public final class NetworkingUtils {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final Environment ENVIRONMENT = new Environment();
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String JWT_TOKEN_FIELD = "access_token";
    public static final String COGNITO_HOST = ENVIRONMENT.readEnv("COGNITO_HOST");
    // This should be equal to the "ClientName" field of in the UserPoolClient entry for the Backend client n the
    // template file
    public static final String BACKEND_USER_POOL_CLIENT_NAME = "BackendApplicationClient";
    public static final Map<String, String> GRANT_TYPE_CLIENT_CREDENTIALS = Map.of("grant_type", "client_credentials");
    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));

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
