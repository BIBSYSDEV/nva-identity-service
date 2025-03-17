package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.model.ViewingScope.defaultViewingScope;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ViewingScopeTest {

    public static Stream<ViewingScope> viewingScopeProvider() throws BadRequestException {
        return Stream.of(new ViewingScope(Set.of(randomCristinOrgId()), null),
            new ViewingScope(Set.of(randomCristinOrgId()), Set.of(randomCristinOrgId())));
    }

    @Test
    void viewingScopeIsSerializedWithType() throws BadRequestException, IOException {
        ViewingScope viewingScope = randomViewingScope();
        var jsonString = viewingScope.toString();
        var jsonMap = JsonConfig.mapFrom(jsonString);
        assertThat(jsonMap, hasEntry("type", "ViewingScope"));
    }

    private ViewingScope randomViewingScope() throws BadRequestException {
        return ViewingScope.create(Set.of(randomCristinOrgId()), Set.of(randomCristinOrgId()));
    }

    @Test
    void defaultViewingScopeReturnsViewingScope() {
        ViewingScope viewingScope = defaultViewingScope(randomCristinOrgId());
        assertThat(viewingScope.getIncludedUnits().size(), is(equalTo(1)));
    }

    @ParameterizedTest
    @MethodSource("viewingScopeProvider")
    void shouldSerializeAndDeserialize() throws BadRequestException {
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

    @Test
    void shouldDeserializeNullViewingScopeToViewingScopeWithEmptyList() throws JsonProcessingException {
        var json = """
            {
              "type": "ViewingScope"
            }
            """;
        var viewingScope = JsonUtils.dtoObjectMapper.readValue(json, ViewingScope.class);
        assertThat(viewingScope.getExcludedUnits(), is(emptyIterable()));
        assertThat(viewingScope.getIncludedUnits(), is(emptyIterable()));
    }
}