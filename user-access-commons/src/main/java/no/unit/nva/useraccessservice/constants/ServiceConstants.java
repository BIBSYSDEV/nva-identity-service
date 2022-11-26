package no.unit.nva.useraccessservice.constants;

import java.net.URI;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class ServiceConstants {
    
    public static final Environment ENVIRONMENT = new Environment();
    public static final URI CRISTIN_BASE_URI = URI.create(ENVIRONMENT.readEnv("CRISTIN_API_URL"));
    public static final String API_DOMAIN = ENVIRONMENT.readEnv("API_DOMAIN");
    public static final String CRISTIN_PATH = "/cristin/organization/";
    
    private ServiceConstants() {
    
    }
}
