package no.unit.useraccessserivce.accessrights;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class AccessRightTest {

    public static final String APPROVE_DOI_REQUEST_STRING = "\"APPROVE_DOI_REQUEST\"";

    @Test
    public void fromStringParsesStringCaseInsensitive() {
        String variableCase = "apProve_DOi_reQueST";
        AccessRight actualValue = AccessRight.fromString(variableCase);
        assertThat(actualValue, is(equalTo(AccessRight.APPROVE_DOI_REQUEST)));
    }

    @Test
    public void fromStringThrowsExceptoinWhenInputIsInvalidAccessRight() {
        String invalidAccessRight = "invalidAccessRight";
        Executable action = () -> AccessRight.fromString(invalidAccessRight);
        InvalidAccessRightException exception = assertThrows(InvalidAccessRightException.class, action);
        assertThat(exception.getMessage(), containsString(invalidAccessRight));
    }

    @Test
    public void accessRightIsSerializedUpperCase() throws JsonProcessingException {
        String value = JsonUtils.objectMapper.writeValueAsString(AccessRight.APPROVE_DOI_REQUEST);
        String expectedValue = APPROVE_DOI_REQUEST_STRING;
        assertThat(value, is(equalTo(expectedValue)));
    }

    @Test
    public void accessRightIsDeserializedFromString() throws IOException {
        String accessRightString = APPROVE_DOI_REQUEST_STRING;
        AccessRight accessRight = JsonUtils.objectMapper.readValue(accessRightString, AccessRight.class);
        assertThat(accessRight, is(equalTo(AccessRight.APPROVE_DOI_REQUEST)));
    }
}