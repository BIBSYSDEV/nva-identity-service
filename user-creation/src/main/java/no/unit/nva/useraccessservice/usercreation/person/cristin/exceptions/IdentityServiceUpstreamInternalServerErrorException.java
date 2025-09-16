package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes.UPSTREAM_INTERNAL_ERROR;

public class IdentityServiceUpstreamInternalServerErrorException extends IdentityServiceException {
    public IdentityServiceUpstreamInternalServerErrorException(String message) {
        super(UPSTREAM_INTERNAL_ERROR, message);
    }

    public IdentityServiceUpstreamInternalServerErrorException(String message, Throwable cause) {
        super(UPSTREAM_INTERNAL_ERROR, message, cause);
    }
}