package no.unit.nva.customer;

import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;

public final class RestConfig {

    public static final JSON defaultRestObjectMapper = JSON.builder()
        .register(JacksonAnnotationExtension.std)
        .build();

    private RestConfig() {

    }

}
