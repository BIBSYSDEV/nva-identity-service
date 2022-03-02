package no.unit.nva.useraccessmanagement.interfaces;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class TypedTest {

    @Test
    void shouldThrowExceptionWhenInputTypeIsDifferentFromHardcodedType() {
        var expectedType = randomString();
        String illegalType = randomString();
        var typed = new Typed() {

            @Override
            public String getType() {
                return expectedType;
            }
        };

        var exception = assertThrows(IllegalArgumentException.class, () -> typed.setType(illegalType));
        assertThat(exception.getMessage(), allOf(containsString(expectedType), containsString(illegalType)));
    }
}