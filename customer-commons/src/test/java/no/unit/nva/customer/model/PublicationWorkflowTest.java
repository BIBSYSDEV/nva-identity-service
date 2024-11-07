package no.unit.nva.customer.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static no.unit.nva.customer.model.PublicationWorkflow.DELIMITER;
import static no.unit.nva.customer.model.PublicationWorkflow.ERROR_MESSAGE_TEMPLATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class PublicationWorkflowTest {

    public static final String NONSENSE = "nonsense";

    @Test
    void shouldThrowRuntimeExceptionWhenInputIsInvalid() {
        Executable executable = () -> PublicationWorkflow.lookUp(NONSENSE);
        var exception = assertThrows(RuntimeException.class, executable);
        var expected = format(ERROR_MESSAGE_TEMPLATE, NONSENSE, stream(PublicationWorkflow.values())
                .map(PublicationWorkflow::toString).collect(joining(DELIMITER)));
        assertThat(exception.getMessage(), is(equalTo(expected)));
    }
}