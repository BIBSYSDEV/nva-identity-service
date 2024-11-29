package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.net.URI;
import java.util.Arrays;

import static java.util.Objects.isNull;

public enum ApplicationDomain {

    NVA(URI.create("nva.unit.no"));
    private final URI uri;

    @JsonCreator
    ApplicationDomain(URI uri) {
        this.uri = uri;
    }

    public static ApplicationDomain fromUri(URI candidate) {
        var uri = mapValuesFromPreviousDatamodel(candidate);
        return Arrays.stream(ApplicationDomain.values())
            .filter(applicationDomain -> applicationDomain.getUri().equals(uri))
            .collect(SingletonCollector.collect());
    }

    private static URI mapValuesFromPreviousDatamodel(URI uri) {
        return isNull(uri) || uri.toString().isEmpty() ? NVA.getUri() : uri;
    }

    public URI getUri() {
        return this.uri;
    }

    @JsonValue
    @Override
    public String toString() {
        return uri.toString();
    }
}
