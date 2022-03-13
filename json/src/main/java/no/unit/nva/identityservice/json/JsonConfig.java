package no.unit.nva.identityservice.json;

import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;

public final class JsonConfig {

    public static final JSON objectMapper = JSON.builder()
        .register(JacksonAnnotationExtension.std)
        .disable(Feature.PRESERVE_FIELD_ORDERING)
        .build();

    private JsonConfig() {

    }

}
