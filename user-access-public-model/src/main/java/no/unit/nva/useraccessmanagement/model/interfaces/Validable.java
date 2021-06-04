package no.unit.nva.useraccessmanagement.model.interfaces;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;

public interface Validable {

    @JsonIgnore
    boolean isValid();

    @JsonIgnore
    default boolean isInvalid() {
        return !isValid();
    }

    InvalidInputException exceptionWhenInvalid();
}
