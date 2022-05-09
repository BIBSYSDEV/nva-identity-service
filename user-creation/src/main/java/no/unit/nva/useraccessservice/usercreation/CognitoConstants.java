package no.unit.nva.useraccessservice.usercreation;

import java.net.URI;
import java.net.URISyntaxException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class CognitoConstants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final URI COGNITO_HOST = defaultCognitoHost();
    public static final String COGNITO_CREDENTIALS_SECRET_NAME = ENVIRONMENT.readEnv("COGNITO_CREDENTIALS_SECRET_NAME");
    public static final String COGNITO_ID_KEY = ENVIRONMENT.readEnv("COGNITO_ID_KEY");
    public static final String COGNITO_SECRET_KEY = ENVIRONMENT.readEnv("COGNITO_SECRET_KEY");

    private CognitoConstants() {

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
