package no.unit.nva.useraccessservice.exceptions;

import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.net.HttpURLConnection;

public class DataSyncException extends ApiGatewayException {

    public DataSyncException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_INTERNAL_ERROR;
    }
}
