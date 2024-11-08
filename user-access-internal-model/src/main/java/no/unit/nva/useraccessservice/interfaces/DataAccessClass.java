package no.unit.nva.useraccessservice.interfaces;

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
     * <p>
     * Its intended use, is to control access to the object.
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
    static void validateBeforePersist(DataAccessClass<?> item) {
        validateBeforeFetch(item);
        //        if (isNull(item.created())) {
        //            throw new IllegalArgumentException("created not set");
        //        }
        if (isNull(item.modified())) {
            throw new IllegalArgumentException("modified not set");
        }
        if (isNull(item.owner())) {
            throw new IllegalArgumentException("owner not set");
        }
        //        if (isNull(item.modifiedBy())) {
        //            throw new IllegalArgumentException("modifiedBy not set");
        //        }
    }

    /**
     * Validates that every field needed to fetch the object is present.
     *
     * @param item the object to validate
     **/
    static void validateBeforeFetch(DataAccessClass<?> item) {
        if (isNull(item.id())) {
            throw new IllegalArgumentException("id not set");
        }
        //        if (isNull(item.type())) {
        //            throw new IllegalArgumentException("type not set");
        //        }
    }

}
