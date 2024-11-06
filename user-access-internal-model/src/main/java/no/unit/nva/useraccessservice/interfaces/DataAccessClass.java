package no.unit.nva.useraccessservice.interfaces;

import java.net.URI;
import java.time.Instant;

public interface DataAccessClass<T extends DataAccessClass<T>> {
    URI withId();

    String withType();

    Instant created();

    Instant modified();

    URI modifiedBy();

    T merge(T item);
}
