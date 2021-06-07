package no.unit.nva.customer.exception;

import nva.commons.exceptions.ApiGatewayException;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;

public class InputException extends ApiGatewayException {

    public InputException(String message, Exception exception) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return SC_BAD_REQUEST;
    }
}
