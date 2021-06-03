package no.unit.nva.useraccessmanagement.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class NotAuthorizedExceptionTest {

    public static final String SOME_MESAGE = "Some message";

    @Test
    public void notAuthorizedExceptionReturnsUnauthorizedStatusCode() {
        NotAuthorizedException exception = new NotAuthorizedException(SOME_MESAGE);
        assertThat(exception.getStatusCode(), is(equalTo(HttpStatus.SC_UNAUTHORIZED)));
    }

    @Test
    public void notAuthorizedExceptionStackTraceContainsSuppliedMessage() {
        NotAuthorizedException exception = new NotAuthorizedException(SOME_MESAGE);
        assertThat(exception.getMessage(), containsString(SOME_MESAGE));
    }

}