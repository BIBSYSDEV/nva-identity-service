package no.unit.nva.customer.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static no.unit.nva.customer.model.ChannelConstraint.DELIMITER;
import static no.unit.nva.customer.model.ChannelConstraint.ERROR_MESSAGE_TEMPLATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class ChannelConstraintTest {
    public static final String NONSENSE = "nonsense";

    @Test
    void shouldThrowRuntimeExceptionWhenInputIsInvalid() {
        Executable executable = () -> ChannelConstraint.lookUp(NONSENSE);
        var exception = assertThrows(RuntimeException.class, executable);
        var expected = format(ERROR_MESSAGE_TEMPLATE, NONSENSE, stream(ChannelConstraint.values())
                .map(ChannelConstraint::toString).collect(joining(DELIMITER)));
        assertThat(exception.getMessage(), is(equalTo(expected)));
    }
}