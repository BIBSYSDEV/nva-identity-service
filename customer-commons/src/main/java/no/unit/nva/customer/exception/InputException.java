package no.unit.nva.customer.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.net.HttpURLConnection;

public class InputException extends ApiGatewayException {

    public InputException(String message, Exception exception) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }
}
