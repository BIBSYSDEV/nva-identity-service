package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.UPSTREAM_REDIRECT;

import java.net.URI;

public class IdentityServiceRedirectException extends IdentityServiceException {

    private static final String MESSAGE_FORMAT = "Request to %s was redirected to %s (HTTP %d)";

    public IdentityServiceRedirectException(URI fromUri, String toLocation, int statusCode) {
        super(UPSTREAM_REDIRECT, MESSAGE_FORMAT.formatted(fromUri, toLocation, statusCode));
    }
}
