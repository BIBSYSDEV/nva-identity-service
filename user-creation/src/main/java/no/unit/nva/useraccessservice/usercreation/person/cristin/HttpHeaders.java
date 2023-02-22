package no.unit.nva.useraccessservice.usercreation.person.cristin;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class HttpHeaders {
    private final Map<String, String> headers = new ConcurrentHashMap<>();

    public HttpHeaders withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Stream<Entry<String, String>> stream() {
        return headers.entrySet().stream();
    }
}
