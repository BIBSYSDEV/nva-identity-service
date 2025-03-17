package no.unit.nva.useraccessservice.model.interfaces;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;

public interface Validable {

    @JsonIgnore
    default boolean isInvalid() {
        return !isValid();
    }

    @JsonIgnore
    boolean isValid();

    InvalidInputException exceptionWhenInvalid();
}
