package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.UPSTREAM_PARSING_ERROR;

public class IdentityServiceUpstreamBodyParsingException extends IdentityServiceException {
    public IdentityServiceUpstreamBodyParsingException(String message, Throwable cause) {
        super(UPSTREAM_PARSING_ERROR, message, cause);
    }
}