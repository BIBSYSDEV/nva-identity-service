package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.Arrays;

public enum ApplicationDomain {

    NVA(URI.create("https://nva.org"));

    private final URI uri;

    @JsonCreator
    ApplicationDomain(URI uri) {
        this.uri = uri;
    }

    public static ApplicationDomain fromUri(URI uri) {
        return Arrays.stream(ApplicationDomain.values()).filter(i -> i.uri.equals(uri)).findFirst().orElseThrow();
    }

    @JsonValue
    @Override
    public String toString() {
        return this.uri.toString();
    }
}
