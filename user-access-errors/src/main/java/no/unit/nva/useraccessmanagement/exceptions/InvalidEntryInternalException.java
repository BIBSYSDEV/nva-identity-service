package no.unit.nva.useraccessmanagement.exceptions;

public class InvalidEntryInternalException extends RuntimeException {

    public InvalidEntryInternalException(String message) {
        super(message);
    }

    public InvalidEntryInternalException(Exception exception) {
        super(exception);
    }


}
