package no.unit.nva.cognito.exception;

public class BadGatewayException extends RuntimeException {

    public BadGatewayException(String message) {
        super(message);
    }

    public BadGatewayException(String message, Exception cause) {
        super(message,cause);
    }
}
