package no.unit.nva.useraccessservice.model;

import no.unit.nva.identityservice.json.JsonConfig;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Not a final class in order to show where these values methods are used through inheritance.
@SuppressWarnings("NonFinalUtilityClass")
public class DtoTest {

    protected static final String JSON_TYPE_ATTRIBUTE = "type";

    protected static void assertThatSerializedItemContainsType(Map<String, Object> json, String expectedTypeValue) {
        var jsonTypeAttribute = json.get(JSON_TYPE_ATTRIBUTE);
        assertNotNull(jsonTypeAttribute);
        assertThat(jsonTypeAttribute.toString(), is(equalTo(expectedTypeValue)));
    }

    protected Map<String, Object> toMap(Object serializable) throws IOException {
        var json = serializable.toString();
        return JsonConfig.mapFrom(json);
    }
}
