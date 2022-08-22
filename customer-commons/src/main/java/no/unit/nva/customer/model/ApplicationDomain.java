package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.Arrays;
import nva.commons.core.SingletonCollector;

public enum ApplicationDomain {
    
    NVA(URI.create("https://nva.org"));
    
    private final URI uri;
    
    @JsonCreator
    ApplicationDomain(URI uri) {
        this.uri = uri;
    }
    
    public static ApplicationDomain fromUri(URI candidate) {
        return Arrays.stream(ApplicationDomain.values())
                   .filter(applicationDomain -> applicationDomain.getUri().equals(candidate))
                   .collect(SingletonCollector.collect());
    }
    
    @JsonValue
    @Override
    public String toString() {
        return this.uri.toString();
    }
    
    public URI getUri() {
        return this.uri;
    }
}
