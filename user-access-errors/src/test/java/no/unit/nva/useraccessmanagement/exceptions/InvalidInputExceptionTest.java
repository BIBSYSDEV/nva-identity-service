package no.unit.nva.useraccessmanagement.exceptions;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

class InvalidInputExceptionTest {

    public static final String SOME_MESSAGE = "Some message";

    @Test
    public void statusCodeReturnsBadRequest() {
        InvalidInputException invalidInputException = new InvalidInputException(SOME_MESSAGE);
        assertThat(invalidInputException.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    public void exceptionIncludesSuppliedMessageInExceptionMessage() {
        InvalidInputException invalidInputException = new InvalidInputException(SOME_MESSAGE);
        assertThat(invalidInputException.getMessage(), containsString(SOME_MESSAGE));
    }
}