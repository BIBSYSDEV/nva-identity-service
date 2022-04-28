package no.unit.nva.cognito;

import java.net.URI;
import java.net.URISyntaxException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.regions.Region;

@JacocoGenerated
public final class NetworkingUtils {

    public static final Environment ENVIRONMENT = new Environment();
    public static final URI COGNITO_HOST = defaultCognitoHost();
    public static final String API_DOMAIN = ENVIRONMENT.readEnv("API_DOMAIN");
    public static final URI CRISTIN_HOST = UriWrapper.fromHost(API_DOMAIN).addChild("cristin").getUri();
    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));
    public static final String COGNITO_CREDENTIALS_SECRET_NAME = ENVIRONMENT.readEnv("COGNITO_CREDENTIALS_SECRET_NAME");
    public static final String COGNITO_ID_KEY = ENVIRONMENT.readEnv("COGNITO_ID_KEY");
    public static final String COGNITO_SECRET_KEY = ENVIRONMENT.readEnv("COGNITO_SECRET_KEY");
    public static final String APPLICATION_JSON = "application/json";

    private NetworkingUtils() {

    }

    @JacocoGenerated
    private static URI defaultCognitoHost() {
        try {
            return new URI("https", ENVIRONMENT.readEnv("COGNITO_HOST"), null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
