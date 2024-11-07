package no.unit.nva.useraccessservice.interfaces;

import java.net.URI;
import java.time.Instant;

import static java.util.Objects.isNull;

public interface DataAccessClass<T extends DataAccessClass<T>> {
    URI id();

    String type();

    Instant created();

    Instant modified();

    URI modifiedBy();

    /**
     * Merge the current object with the new object.
     *
     * @param item the new object to merge with
     * @return the merged object
     */
    T merge(T item);

    /**
     * Validates that every field needed to persist the object is present.
     *
     * @param item the object to validate
     **/
    static void validateBeforePersist(DataAccessClass<?> item) throws IllegalArgumentException {
        validateBeforeFetch(item);
        if (isNull(item.created())) {
            throw new IllegalArgumentException("created not set");
        }
        if (isNull(item.modified())) {
            throw new IllegalArgumentException("modified not set");
        }
        if (isNull(item.modifiedBy())) {
            throw new IllegalArgumentException("modifiedBy not set");
        }
    }

    /**
     * Validates that every field needed to fetch the object is present.
     *
     * @param item the object to validate
     **/
    static void validateBeforeFetch(DataAccessClass<?> item) throws IllegalArgumentException {
        if (isNull(item.id())) {
            throw new IllegalArgumentException("id not set");
        }
        if (isNull(item.type())) {
            throw new IllegalArgumentException("type not set");
        }
    }

}
