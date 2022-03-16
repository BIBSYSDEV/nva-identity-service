package no.unit.nva.useraccessservice.exceptions;

import java.net.HttpURLConnection;
import nva.commons.apigatewayv2.exceptions.ApiGatewayException;

public class NotAuthorizedException extends ApiGatewayException {

    public NotAuthorizedException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_UNAUTHORIZED;
    }
}
