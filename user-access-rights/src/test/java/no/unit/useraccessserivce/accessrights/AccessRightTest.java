package no.unit.useraccessserivce.accessrights;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class AccessRightTest {

    public static final String APPROVE_DOI_REQUEST_STRING = "\"APPROVE_DOI_REQUEST\"";
    public static final JSON objectMapper = JSON.std;

    @Test
    void fromStringParsesStringCaseInsensitive() {
        String variableCase = "apProve_DOi_reQueST";
        AccessRight actualValue = AccessRight.fromString(variableCase);
        assertThat(actualValue, is(equalTo(AccessRight.APPROVE_DOI_REQUEST)));
    }

    @Test
    void fromStringThrowsExceptoinWhenInputIsInvalidAccessRight() {
        String invalidAccessRight = "invalidAccessRight";
        Executable action = () -> AccessRight.fromString(invalidAccessRight);
        InvalidAccessRightException exception = assertThrows(InvalidAccessRightException.class, action);
        assertThat(exception.getMessage(), containsString(invalidAccessRight));
    }

    @Test
    void accessRightIsSerializedUpperCase() throws IOException {
        String value = objectMapper.asString(AccessRight.APPROVE_DOI_REQUEST);
        assertThat(value, is(equalTo(APPROVE_DOI_REQUEST_STRING)));
    }

    @Test
    void accessRightIsDeserializedFromString() throws IOException {
        AccessRight accessRight = objectMapper.beanFrom(AccessRight.class,APPROVE_DOI_REQUEST_STRING);
        assertThat(accessRight, is(equalTo(AccessRight.APPROVE_DOI_REQUEST)));
    }
}