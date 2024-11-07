package no.unit.nva.useraccessservice.exceptions;

import nva.commons.apigateway.exceptions.ApiGatewayException;

import javax.net.ssl.HttpsURLConnection;

public class InvalidInputException extends ApiGatewayException {

    public InvalidInputException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpsURLConnection.HTTP_BAD_REQUEST;
    }
}
