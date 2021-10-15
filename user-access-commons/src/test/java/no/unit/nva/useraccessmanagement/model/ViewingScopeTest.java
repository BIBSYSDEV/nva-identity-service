package no.unit.nva.useraccessmanagement.model;

import static com.spotify.hamcrest.jackson.IsJsonObject.jsonObject;
import static com.spotify.hamcrest.jackson.IsJsonText.jsonText;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import no.unit.nva.useraccessmanagement.DynamoConfig;
import org.junit.jupiter.api.Test;

public class ViewingScopeTest {

    @Test
    void viewingScopeIsSerializedWithType() {
        ViewingScope viewingScope = new ViewingScope(Set.of(randomUri()), Set.of(randomUri()));
        ObjectNode json = DynamoConfig.defaultDynamoConfigMapper.convertValue(viewingScope, ObjectNode.class);
        assertThat(json, is(jsonObject().where("type", is(jsonText("ViewingScope")))));
    }
}