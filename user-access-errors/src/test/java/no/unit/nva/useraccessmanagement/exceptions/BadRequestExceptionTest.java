package no.unit.nva.useraccessmanagement.exceptions;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

class BadRequestExceptionTest {

    public static final String SOME_MESSAGE = "Some message";

    @Test
    public void statusCodeReturnsBadRequest() {
        BadRequestException exception = new BadRequestException(SOME_MESSAGE);
        assertThat(exception.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    public void exceptionIncludesSuppliedMessageInExceptionMessage() {
        BadRequestException exception = new BadRequestException(SOME_MESSAGE);
        assertThat(exception.getMessage(), containsString(SOME_MESSAGE));
    }
}