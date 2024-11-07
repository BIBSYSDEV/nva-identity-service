package no.unit.nva.customer.model.interfaces;

import nva.commons.apigateway.exceptions.BadRequestException;

public interface Typed {

    String TYPE_FIELD = "type";

    private static String errorMessage(String unexpectedType, String expectedType) {
        return String.format("Unexpected type:%s. Expected type is:%s", unexpectedType, expectedType);
    }

    String getType();

    default void setType(String type) throws BadRequestException {
        if (!getType().equals(type)) {
            throw new BadRequestException(errorMessage(type, getType()));
        }
    }
}
