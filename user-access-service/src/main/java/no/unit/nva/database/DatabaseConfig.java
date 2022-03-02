package no.unit.nva.database;

import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;

public final class DatabaseConfig {

    public static final JSON Json = JSON.builder().register(JacksonAnnotationExtension.std).build();

    private DatabaseConfig() {

    }
}
