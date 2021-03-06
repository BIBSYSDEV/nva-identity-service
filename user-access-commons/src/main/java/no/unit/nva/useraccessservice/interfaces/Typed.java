package no.unit.nva.useraccessservice.interfaces;

import nva.commons.apigateway.exceptions.BadRequestException;

public interface Typed {

    String TYPE_FIELD = "type";

    String getType();

    default void setType(String type) throws BadRequestException {
        if (!getType().equals(type)) {
            throw new BadRequestException(errorMessage(type));
        }
    }

    private String errorMessage(String type) {
        return String.format("Unexpected type: %s.Expected type: %s", type, getType());
    }
}
