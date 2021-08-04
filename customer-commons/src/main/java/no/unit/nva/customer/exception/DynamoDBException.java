package no.unit.nva.customer.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;

import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;

public class DynamoDBException extends ApiGatewayException {

    public DynamoDBException(String message, Exception exception) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return SC_BAD_GATEWAY;
    }
}
