package no.unit.nva.useraccessservice.exceptions;

import java.net.HttpURLConnection;
import nva.commons.apigatewayv2.exceptions.ApiGatewayException;

public class DataSyncException extends ApiGatewayException {

    public DataSyncException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_INTERNAL_ERROR;
    }
}
