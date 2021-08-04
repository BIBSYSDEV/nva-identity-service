package no.unit.useraccessserivce.accessrights;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import org.junit.jupiter.api.Test;

public class InvalidAccessRightExceptionTest {

    public static final String INVALID_ACCESS_RIGHT = "Invalid";

    @Test
    public void invalidAccessRightExceptionMessageContainsTheInvalidAccessRight() {
        InvalidAccessRightException exception = new InvalidAccessRightException(INVALID_ACCESS_RIGHT);
        assertThat(exception.getMessage(), containsString(INVALID_ACCESS_RIGHT));
    }
}