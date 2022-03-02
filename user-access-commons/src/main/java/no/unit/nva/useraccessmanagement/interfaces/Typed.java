package no.unit.nva.useraccessmanagement.interfaces;

public interface Typed {

    String TYPE_FIELD = "type";

    String getType();

    default void setType(String type) {
        if (!getType().equals(type)) {
            throw new IllegalArgumentException(errorMessage(type));
        }
    }

    private String errorMessage(String type) {
        return String.format("Unexpected type: %s.Expected type: %s", type, getType());
    }
}
