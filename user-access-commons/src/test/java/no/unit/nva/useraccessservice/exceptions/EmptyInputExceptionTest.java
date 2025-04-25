package no.unit.nva.useraccessservice.exceptions;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class EmptyInputExceptionTest {

    public static final String SOME_MESSAGE = "Some message";

    @Test
    public void statusCodeReturnsBadRequest() {
        EmptyInputException emptyInputException = new EmptyInputException(SOME_MESSAGE);
        assertThat(emptyInputException.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    public void exceptionIncludesSuppliedMessageInExceptionMessage() {
        EmptyInputException emptyInputException = new EmptyInputException(SOME_MESSAGE);
        assertThat(emptyInputException.getMessage(), containsString(SOME_MESSAGE));
    }
}