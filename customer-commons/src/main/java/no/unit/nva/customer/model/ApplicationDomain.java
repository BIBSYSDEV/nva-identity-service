package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;

public enum ApplicationDomain {

    NVA(URI.create("https://nva.org"));

    private final URI uri;

    @JsonCreator
    ApplicationDomain(URI uri) {
        this.uri = uri;
    }

    @JsonValue
    @Override
    public String toString() {
        return this.uri.toString();
    }
}
