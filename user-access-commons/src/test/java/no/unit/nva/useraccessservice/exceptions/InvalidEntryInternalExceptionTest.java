package no.unit.nva.useraccessservice.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InvalidEntryInternalExceptionTest {

    private static final String SOME_MESSAGE = "SomeMessage";

    @Test
    public void invalidInternalExceptionHasConstructorWithMessage() {
        Executable action = () -> {
            throw new InvalidEntryInternalException(SOME_MESSAGE);
        };
        InvalidEntryInternalException exception = assertThrows(InvalidEntryInternalException.class, action);
        assertThat(exception.getMessage(), containsString(SOME_MESSAGE));
    }


    @Test
    public void invalidEntryInternalExceptionContainsCause() {
        Exception cause = new Exception(SOME_MESSAGE);
        InvalidEntryInternalException exception = new InvalidEntryInternalException(cause);
        assertThat(exception.getCause(), is(equalTo(cause)));
    }
}