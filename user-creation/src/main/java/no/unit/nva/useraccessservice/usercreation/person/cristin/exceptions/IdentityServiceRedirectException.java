package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import java.net.URI;

public class IdentityServiceRedirectException extends RuntimeException {

    private static final String MESSAGE_FORMAT = "Request to %s was redirected to %s (HTTP %d)";

    public IdentityServiceRedirectException(URI fromUri, String toLocation, int statusCode) {
        super(MESSAGE_FORMAT.formatted(fromUri, toLocation, statusCode));
    }
}
