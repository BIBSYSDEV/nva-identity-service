package no.unit.nva.useraccessmanagement.model;

import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;

final class PublicModelJsonConfig {

    public static final JSON objectMapper = JSON.builder().register(JacksonAnnotationExtension.std).build();

    private PublicModelJsonConfig() {

    }
}
