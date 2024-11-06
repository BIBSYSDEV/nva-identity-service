package no.unit.nva.customer.model.interfaces;

import nva.commons.apigateway.exceptions.NotFoundException;

public interface DataAccessLayer<T extends DataAccessClass<T>> {

    T upsert(DataAccessService<T> service) throws NotFoundException;

    T fetch(DataAccessService<T> service) throws NotFoundException;

}
