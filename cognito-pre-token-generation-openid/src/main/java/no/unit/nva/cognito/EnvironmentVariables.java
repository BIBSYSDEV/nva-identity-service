package no.unit.nva.cognito;

import java.net.URI;
import java.net.URISyntaxException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.regions.Region;

public final class EnvironmentVariables {

    public static final Environment ENVIRONMENT = new Environment();
    public static final URI COGNITO_HOST = defaultCognitoHost();
    public static final String API_DOMAIN = ENVIRONMENT.readEnv("API_DOMAIN");
    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));
    private EnvironmentVariables() {
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
