package no.unit.nva.customer.model;

import no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChannelConstraintPolicyTest {
    private final String NONSENSE = "nonsense";
    private final String ERROR_MESSAGE_TEMPLATE = "%s not a valid ChannelConstraintPolicy, expected one of: %s";
    private final String DELIMITER = ", ";

    @Test
    void shouldThrowRuntimeExceptionWhenInputIsInvalid() {
        Executable executable = () -> ChannelConstraintPolicy.lookUp(NONSENSE);
        var exception = assertThrows(RuntimeException.class, executable);
        var expected = format(ERROR_MESSAGE_TEMPLATE, NONSENSE, stream(ChannelConstraintPolicy.values())
                .map(ChannelConstraintPolicy::toString).collect(joining(DELIMITER)));
        assertThat(exception.getMessage(), is(equalTo(expected)));
    }
}