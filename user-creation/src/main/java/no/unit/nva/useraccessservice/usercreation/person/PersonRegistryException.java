package no.unit.nva.useraccessservice.usercreation.person;

public class PersonRegistryException extends RuntimeException {

    public PersonRegistryException(String message) {
        super(message);
    }

    public PersonRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
