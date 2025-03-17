package no.unit.nva.database.interfaces;

import nva.commons.apigateway.exceptions.NotFoundException;

/**
 * Interface for data access services.
 *
 * @param <T> the type of the {@link DataAccessClass <T> }  interface.
 */
public interface DataAccessService<T extends DataAccessClass<T>> {

    String RESOURCE_NOT_FOUND_MESSAGE = "The entity you're looking for, is not here...";

    /**
     * Persists an item in the database.
     *
     * @param item the item to persist.
     * @throws IllegalArgumentException before persisting, if the item is incomplete.
     */
    void persist(T item);

    /**
     * Fetches an item from the database.
     *
     * @param item the item to fetch.
     *             The item must contain id and type, defined in the DataAccessClass interface.
     * @return the fetched item.
     * @throws IllegalArgumentException if the item doesn't contain valid lookup keys.
     * @throws NotFoundException        if the item does not exist.
     */
    T fetch(T item) throws NotFoundException;


    /**
     * Deletes an item from the database.
     *
     * @param item the item to delete.
     * @throws IllegalArgumentException if the item doesn't contain valid lookup keys.
     * @throws NotFoundException        if the item does not exist.
     */
    void delete(T item) throws NotFoundException;
}