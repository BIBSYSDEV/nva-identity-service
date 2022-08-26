package no.unit.nva.customer.model;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.Arrays;
import nva.commons.core.SingletonCollector;

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

    @JsonValue
    @Override
    public String toString() {
        return uri.toString();
    }

    public URI getUri() {
        return this.uri;
    }

    private static URI mapValuesFromPreviousDatamodel(URI uri) {
        return isNull(uri) || uri.toString().isEmpty() ? NVA.getUri() : uri;
    }
}
