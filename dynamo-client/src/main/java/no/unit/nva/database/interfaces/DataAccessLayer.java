package no.unit.nva.database.interfaces;

import nva.commons.apigateway.exceptions.NotFoundException;

/**
 * Interface for data access layers.
 *
 * @param <T> the type of the {@link DataAccessClass <T> }  interface.
 */
public interface DataAccessLayer<T extends DataAccessClass<T>> {

    /**
     * Persists the object in the database.
     *
     * @param service the service that will persist the object.
     * @return the object that was persisted.
     * @throws NotFoundException if the object was not found.
     */
    T upsert(DataAccessService<T> service) throws NotFoundException;

    /**
     * Fetches the object from the database.
     *
     * @param service the service that will fetch the object.
     * @return the object that was fetched.
     * @throws NotFoundException if the object was not found.
     */
    T fetch(DataAccessService<T> service) throws NotFoundException;

}
