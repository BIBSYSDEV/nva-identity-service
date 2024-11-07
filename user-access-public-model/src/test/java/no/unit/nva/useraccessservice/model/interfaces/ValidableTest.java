package no.unit.nva.useraccessservice.model.interfaces;

import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class ValidableTest {

    @Test
    public void isInvalidReturnsOppositeOfIsValid() {
        Validable validable = sampleValidable();
        assertThat(validable.isInvalid(), is(equalTo(!validable.isValid())));
    }

    private Validable sampleValidable() {
        return new Validable() {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public InvalidInputException exceptionWhenInvalid() {
                return null;
            }
        };
    }
}