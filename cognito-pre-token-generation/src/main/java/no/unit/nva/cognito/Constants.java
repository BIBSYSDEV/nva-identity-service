package no.unit.nva.cognito;

import com.amazonaws.regions.Regions;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public final class Constants {

    public static final Environment ENVIRONMENT = new Environment();

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    public static final String AWS_REGION_ENV = "AWS_REGION";

    public static final String ID_NAMESPACE_VALUE = ENVIRONMENT.readEnv(ID_NAMESPACE_ENV);
    public static final Regions AWS_REGION_VALUE = setupRegion();

    private Constants() {
    }

    @JacocoGenerated
    private static Regions setupRegion() {
        return ENVIRONMENT.readEnvOpt(AWS_REGION_ENV).map(Regions::fromName).orElse(Regions.EU_WEST_1);
    }

}
