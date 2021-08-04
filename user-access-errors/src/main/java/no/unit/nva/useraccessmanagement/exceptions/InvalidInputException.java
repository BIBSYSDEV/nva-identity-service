package no.unit.nva.useraccessmanagement.exceptions;

import javax.net.ssl.HttpsURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class InvalidInputException extends ApiGatewayException {

    public InvalidInputException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpsURLConnection.HTTP_BAD_REQUEST;
    }
}
