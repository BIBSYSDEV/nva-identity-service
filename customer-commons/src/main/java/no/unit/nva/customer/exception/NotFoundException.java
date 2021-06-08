package no.unit.nva.customer.exception;

import nva.commons.exceptions.ApiGatewayException;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

public class NotFoundException extends ApiGatewayException {

    public NotFoundException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return SC_NOT_FOUND;
    }
}
