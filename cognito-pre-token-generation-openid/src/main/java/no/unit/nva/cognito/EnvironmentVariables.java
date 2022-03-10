package no.unit.nva.cognito;

import nva.commons.core.Environment;
import software.amazon.awssdk.regions.Region;

public final class EnvironmentVariables {

    public static final Environment ENVIRONMENT = new Environment();
    public static final String COGNITO_HOST = ENVIRONMENT.readEnv("COGNITO_HOST");
    public static final String API_DOMAIN = ENVIRONMENT.readEnv("API_DOMAIN");
    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));

    private EnvironmentVariables() {
    }
}
