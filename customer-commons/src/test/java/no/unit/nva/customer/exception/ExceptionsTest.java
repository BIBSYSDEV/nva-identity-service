package no.unit.nva.customer.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;

public class ExceptionsTest {

    public static final String MESSAGE = "Message";

    @Test
    public void inputExceptionHasStatusCode() {
        ApiGatewayException exception = new InputException(MESSAGE, new RuntimeException());
        Assertions.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, exception.getStatusCode());
    }
}
