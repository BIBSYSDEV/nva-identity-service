package no.unit.nva.useraccessservice.model;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientDtoTest extends DtoTest {

    protected static final String CLIENT_TYPE_LITERAL = "Client";

    @DisplayName("ClientDto object contains type with value \"Client\"")
    @Test
    void clientDtoSerializedObjectContainsTypeWithValueUser() throws IOException {
        ClientDto clientDto = createRandomClient();
        var jsonMap = toMap(clientDto);

        String actualType = jsonMap.get(JSON_TYPE_ATTRIBUTE).toString();
        assertThat(actualType, is(equalTo(CLIENT_TYPE_LITERAL)));
    }

    @DisplayName("ClientDto can be created when it contains the right type value")
    @Test
    void clientDtoCanBeDeserializedWhenItContainsTheRightTypeValue()
        throws InvalidEntryInternalException, IOException {
        ClientDto sampleUser = createRandomClient();
        var json = toMap(sampleUser);
        assertThatSerializedItemContainsType(json, CLIENT_TYPE_LITERAL);

        String jsonStringWithType = JsonConfig.writeValueAsString(json);
        ClientDto deserializedItem = JsonConfig.readValue(jsonStringWithType, ClientDto.class);

        assertThat(deserializedItem, is(equalTo(sampleUser)));
        assertThat(deserializedItem, is(not(sameInstance(sampleUser))));
    }

    @Test
    void shouldCopyWithoutInformationLoss() {
        ClientDto initialUser = createRandomClient();
        ClientDto copiedUser = initialUser.copy().build();

        assertThat(copiedUser, is(equalTo(initialUser)));
        assertThat(copiedUser, is(not(sameInstance(initialUser))));
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

    private static ClientDto createRandomClient() {
        return ClientDto.newBuilder()
                   .withClientId(randomString())
                   .withCustomer(randomCristinOrgId())
                   .build();
    }
}
