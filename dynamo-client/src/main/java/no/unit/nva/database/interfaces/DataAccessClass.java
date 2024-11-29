package no.unit.nva.database.interfaces;

import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.time.Instant;

import static java.util.Objects.isNull;

/**
 * Interface for data access classes, although it has a document based philosophy,
 * it can be used with any database.
 *
 * @param <T> the type of the {@link DataAccessClass<T> }  interface.
 */
public interface DataAccessClass<T extends DataAccessClass<T>> {

    /**
     * Validates that every field needed to persist the object is present.
     *
     * @param item the object to validate
     **/
    @JacocoGenerated
    static void validateBeforePersist(DataAccessClass<?> item) {
        validateBeforeFetch(item);
        // in this implementation, created is always set
        if (isNull(item.created())) {
            throw new IllegalArgumentException("created not set");
        }
        if (isNull(item.modified())) {
            throw new IllegalArgumentException("modified not set");
        }
        if (isNull(item.owner())) {
            throw new IllegalArgumentException("owner not set");
        }
        // in this implementation, modifiedBy is always set
        if (isNull(item.modifiedBy())) {
            throw new IllegalArgumentException("modifiedBy not set");
        }
    }

    /**
     * Returns the time the object was created.
     * <p>It's immutable.</p>
     *
     * @return a UTC time stamp
     */
    Instant created();

    /**
     * <p>Returns current owner of the object.</p>
     * You may wonder why not call this creator,
     * but the creator is the user that created the object,
     * not necessarily the current owner.
     * <p>To find the creator, you will have to look up the history of the object.</p>
     * <p>Its intended use, is to control access to the object.
     *
     * @return an UUID identifier.
     */
    URI owner();

    /**
     * Returns the time the object was last modified.
     *
     * @return a UTC time stamp
     */
    Instant modified();

    /**
     * Returns the identifier of the user that last modified the object.
     *
     * @return an UUID identifier.
     */
    URI modifiedBy();

    /**
     * Validates that every field needed to fetch the object is present.
     *
     * @param item the object to validate
     **/
    @JacocoGenerated
    static void validateBeforeFetch(DataAccessClass<?> item) {
        if (isNull(item.id())) {
            throw new IllegalArgumentException("id not set");
        }
        // in this implementation, type is always set
        if (isNull(item.type())) {
            throw new IllegalArgumentException("type not set");
        }
    }

    /**
     * This is intended to be @DynamoDbPartitionKey if used with DynamoDb.
     * <p>It's immutable.</p>
     *
     * @return an URI id.
     */
    @SuppressWarnings("PMD.ShortMethodName")
    URI id();

    /**
     * This is intended to be @DynamoDbSortKey if used with DynamoDb.
     * <p>It's immutable.</p>
     *
     * @return the type of the object
     */
    String type();

    /**
     * Merge the current object with the new object.
     *
     * @param item the new object to merge with
     * @return the merged object
     */
    T merge(T item);

}
