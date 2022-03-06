package no.unit.nva.useraccessmanagement.model;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessmanagement.model.ViewingScope.defaultViewingScope;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import java.util.Set;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

public class ViewingScopeTest {

    @Test
    void viewingScopeIsSerializedWithType() throws BadRequestException, IOException {
        ViewingScope viewingScope = randomViewingScope();
        var jsonString = viewingScope.toString();
        var jsonMap = objectMapper.mapFrom(jsonString);
        assertThat(jsonMap, hasEntry("type", "ViewingScope"));
    }

    @Test
    void defaultViewingScopeReturnsViewingScope() {
        ViewingScope viewingScope = defaultViewingScope(randomCristinOrgId());
        assertThat(viewingScope.getIncludedUnits().size(), is(equalTo(1)));
    }

    @Test
    void shouldSerializeAndDeserialize() {
        ViewingScope viewingScope = randomViewingScope();
        var json = viewingScope.toString();
        var deserialized = ViewingScope.fromJson(json);
        assertThat(deserialized, is(equalTo(viewingScope)));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenParsingFails() {
        var invalidJson = randomString();
        var exception = assertThrows(BadRequestException.class, () -> ViewingScope.fromJson(invalidJson));
        assertThat(exception.getMessage(), containsString(invalidJson));
    }

    private ViewingScope randomViewingScope() {
        return new ViewingScope(Set.of(randomCristinOrgId()), Set.of(
            randomCristinOrgId()));
    }
}