package no.unit.nva.useraccessservice.exceptions;

import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.net.HttpURLConnection;

public class NotAuthorizedException extends ApiGatewayException {

    public NotAuthorizedException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_UNAUTHORIZED;
    }
}
