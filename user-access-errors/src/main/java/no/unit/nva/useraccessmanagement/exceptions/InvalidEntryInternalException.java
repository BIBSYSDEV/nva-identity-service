package no.unit.nva.useraccessmanagement.exceptions;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class InvalidEntryInternalException extends ApiGatewayException {

    public InvalidEntryInternalException(String message) {
        super(message);
    }

    public InvalidEntryInternalException(Exception exception) {
        super(exception);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_INTERNAL_ERROR;
    }
}
