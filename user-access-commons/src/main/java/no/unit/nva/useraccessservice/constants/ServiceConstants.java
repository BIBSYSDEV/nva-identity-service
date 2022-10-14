package no.unit.nva.useraccessservice.constants;

import java.net.URI;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JacocoGenerated
public final class ServiceConstants {
    
    public static final Environment ENVIRONMENT = new Environment();
    public static final String API_DOMAIN = ENVIRONMENT.readEnv("API_DOMAIN");
    public static final URI CRISTIN_HOST = UriWrapper.fromHost(API_DOMAIN).addChild("cristin").getUri();
    public static final String CRISTIN_PATH = "/cristin/organization/";
    
    private ServiceConstants() {
    
    }
}
