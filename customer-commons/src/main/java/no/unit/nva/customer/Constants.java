package no.unit.nva.customer;

import com.google.common.net.MediaType;
import java.net.URI;
import java.util.List;
import nva.commons.apigateway.MediaTypes;
import nva.commons.core.Environment;

public final class Constants {

    public static final URI ID_NAMESPACE = URI.create(getIdNamespace());

    public static final List<MediaType> DEFAULT_RESPONSE_MEDIA_TYPES = List.of(
        MediaType.JSON_UTF_8,
        MediaTypes.APPLICATION_JSON_LD
    );

    private Constants() {
    }

    private static String getIdNamespace() {
        return new Environment().readEnv("ID_NAMESPACE");
    }
}
