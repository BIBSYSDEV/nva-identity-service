package no.unit.nva.useraccessmanagement.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// Not a final class in order to show where these values methods are used through inheritance.
@SuppressWarnings("NonFinalUtilityClass")
public class DtoTest {

    protected static final String JSON_TYPE_ATTRIBUTE = "type";
    protected static final String INVALID_TYPE_EXCEPTION_MESSAGE_SAMPLE = "Missing type id when trying to resolve "
        + "subtype of";

    protected static void assertThatSerializedItemContainsType(ObjectNode json, String typeLiteral) {
        JsonNode jsonTypeAttribute = json.get(JSON_TYPE_ATTRIBUTE);
        assertNotNull(jsonTypeAttribute);
        assertThat(jsonTypeAttribute.asText(), is(equalTo(typeLiteral)));
    }
}
