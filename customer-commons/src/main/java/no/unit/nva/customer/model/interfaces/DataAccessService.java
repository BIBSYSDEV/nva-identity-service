package no.unit.nva.customer.model.interfaces;

import nva.commons.apigateway.exceptions.NotFoundException;

public interface DataAccessService<T extends DataAccessClass<T>> {

    String RESOURCE_NOT_FOUND_MESSAGE = "Could not find entry";

    void persist(T item) throws NotFoundException;

    T fetch(T item) throws NotFoundException;

}