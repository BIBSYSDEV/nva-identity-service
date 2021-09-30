package no.unit.nva.customer;

import com.google.common.net.MediaType;
import java.util.List;
import nva.commons.apigateway.MediaTypes;

public final class Constants {

    public static final List<MediaType> DEFAULT_RESPONSE_MEDIA_TYPES = List.of(
        MediaType.JSON_UTF_8,
        MediaTypes.APPLICATION_JSON_LD
    );

    private Constants() {
    }
}
