package no.unit.nva.handlers.models;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import java.io.IOException;
import no.unit.nva.identityservice.json.JsonConfig;
import org.junit.jupiter.api.Test;

class CreateUserRequestTest {

    public static final String JSON_TEMPLATE = "{ \"nationalIdentityNumber\": \"%s\"}";

    @Test
    void shouldSerializeAndDeserializeNationalIdentityNumberAsStringButHaveASpecificTypeForIt() throws IOException {
        var nationalIdentityNumber = randomString();
        var serialized = String.format(JSON_TEMPLATE, nationalIdentityNumber);
        var deserialized = JsonConfig.readValue(serialized, CreateUserRequest.class);
        assertThat(deserialized.getNin(), is(equalTo(nationalIdentityNumber)));
    }

    @Test
    void shouldSerializeNationalIdentityNumberAsSimpleString() throws IOException {
        var request = new CreateUserRequest(randomString(), null, null);
        var serialized = request.toString();
        var json = JsonConfig.mapFrom(serialized);
        assertThat(json.get(CreateUserRequest.NATIONAL_IDENTITY_NUMBER_FIELD), is(instanceOf(String.class)));
    }
}