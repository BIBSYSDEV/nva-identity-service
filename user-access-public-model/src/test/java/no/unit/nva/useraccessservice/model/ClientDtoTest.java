package no.unit.nva.useraccessservice.model;

import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientDtoTest extends DtoTest {

    protected static final String CLIENT_TYPE_LITERAL = "Client";

    @DisplayName("ClientDto object contains type with value \"Client\"")
    @Test
    void clientDtoSerializedObjectContainsTypeWithValueClient() throws IOException {
        ClientDto clientDto = createRandomClient();
        var jsonMap = toMap(clientDto);

        String actualType = jsonMap.get(JSON_TYPE_ATTRIBUTE).toString();
        assertThat(actualType, is(equalTo(CLIENT_TYPE_LITERAL)));
    }

    private static ClientDto createRandomClient() {
        return ClientDto.newBuilder()
            .withClientId(randomString())
            .withCustomer(randomCristinOrgId())
            .withCristinOrgUri(randomCristinOrgId())
            .withActingUser(randomString())
            .build();
    }

    @DisplayName("ClientDto can be created when it contains the right type value")
    @Test
    void clientDtoCanBeDeserializedWhenItContainsTheRightTypeValue()
        throws InvalidEntryInternalException, IOException {
        ClientDto sampleClient = createRandomClient();
        var json = toMap(sampleClient);
        assertThatSerializedItemContainsType(json, CLIENT_TYPE_LITERAL);

        String jsonStringWithType = JsonConfig.writeValueAsString(json);
        ClientDto deserializedItem = JsonConfig.readValue(jsonStringWithType, ClientDto.class);

        assertThat(deserializedItem, is(equalTo(sampleClient)));
        assertThat(deserializedItem, is(not(sameInstance(sampleClient))));
    }

    @Test
    void shouldCopyWithoutInformationLoss() {
        ClientDto initialClient = createRandomClient();
        ClientDto copiedClient = initialClient.copy().build();

        assertThat(copiedClient, is(equalTo(initialClient)));
        assertThat(copiedClient, is(not(sameInstance(initialClient))));
    }

    @Test
    void shouldSerializeAsJson() throws BadRequestException {
        var sample = createRandomClient();
        var json = sample.toString();
        var deserialized = ClientDto.fromJson(json);
        assertThat(deserialized, is(equalTo(sample)));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenFailingToDeserialize() {
        var invalidJson = randomString();
        var exception = assertThrows(BadRequestException.class, () -> ClientDto.fromJson(invalidJson));
        assertThat(exception.getMessage(), containsString(invalidJson));
    }
}
