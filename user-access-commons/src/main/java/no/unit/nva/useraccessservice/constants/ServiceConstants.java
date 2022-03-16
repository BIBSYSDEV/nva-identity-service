package no.unit.nva.useraccessservice.constants;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class ServiceConstants {

    public static final String CRISTIN_PATH = "/cristin/organization/";
    private static final Environment ENVIRONMENT = new Environment();
    public static final String API_HOST = ENVIRONMENT.readEnv("API_HOST");

    private ServiceConstants() {

    }
}
