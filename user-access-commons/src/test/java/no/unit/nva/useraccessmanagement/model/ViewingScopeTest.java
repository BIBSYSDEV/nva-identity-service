package no.unit.nva.useraccessmanagement.model;

import static com.spotify.hamcrest.jackson.IsJsonObject.jsonObject;
import static com.spotify.hamcrest.jackson.IsJsonText.jsonText;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessmanagement.model.ViewingScope.DO_NOT_INCLUDE_NESTED_UNITS;
import static no.unit.nva.useraccessmanagement.model.ViewingScope.INCLUDE_NESTED_UNITS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import no.unit.nva.useraccessmanagement.DynamoConfig;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

public class ViewingScopeTest {

    @Test
    void viewingScopeIsSerializedWithType() throws BadRequestException {
        ViewingScope viewingScope = new ViewingScope(Set.of(randomUri()), Set.of(randomUri()),INCLUDE_NESTED_UNITS);
        ObjectNode json = DynamoConfig.defaultDynamoConfigMapper.convertValue(viewingScope, ObjectNode.class);
        assertThat(json, is(jsonObject().where("type", is(jsonText("ViewingScope")))));
    }

    @Test
    void viewingScopeRecursivenessDefaultsToViewingScopeNotBeingRecursive() throws BadRequestException {
        ViewingScope viewingScope = new ViewingScope(Set.of(randomUri()), Set.of(randomUri()),null);
        assertThat(viewingScope.isRecursive(),is(equalTo(DO_NOT_INCLUDE_NESTED_UNITS)));
    }


}