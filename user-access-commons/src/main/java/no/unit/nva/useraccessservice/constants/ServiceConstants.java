package no.unit.nva.useraccessservice.constants;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.URI;

@JacocoGenerated
public final class ServiceConstants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final URI CRISTIN_BASE_URI = URI.create(ENVIRONMENT.readEnv("CRISTIN_API_URL"));
    public static final String API_DOMAIN = ENVIRONMENT.readEnv("API_DOMAIN");
    public static final String CRISTIN_PATH = "/cristin/organization/";
    public static final String BOT_FILTER_BYPASS_HEADER_NAME =
            ENVIRONMENT.readEnv("BOT_FILTER_BYPASS_HEADER_NAME");
    public static final String BOT_FILTER_BYPASS_HEADER_VALUE =
            ENVIRONMENT.readEnv("BOT_FILTER_BYPASS_HEADER_VALUE");

    private ServiceConstants() {

    }
}
