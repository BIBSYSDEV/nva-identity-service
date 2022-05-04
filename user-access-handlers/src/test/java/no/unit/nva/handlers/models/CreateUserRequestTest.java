package no.unit.nva.handlers.models;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import java.io.IOException;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import org.junit.jupiter.api.Test;

class CreateUserRequestTest {

    public static final String JSON_TEMPLATE = "{ \"nationalIdentityNumber\": \"%s\"}";

    @Test
    void shouldSerializeAndDeserializeNationalIdentityNumberAsStringButHaveASpecificTypeForIt() throws IOException {
        var nationalIdentityNumber = new NationalIdentityNumber(randomString());
        var serialized = String.format(JSON_TEMPLATE, nationalIdentityNumber);
        var deserialized = JsonConfig.readValue(serialized, CreateUserRequest.class);
        assertThat(deserialized.getNin(), is(equalTo(nationalIdentityNumber)));
        assertThatNationalIdentityNumberIsNotRepresentedInternallyAsOnlyString(deserialized);
    }

    @Test
    void shouldSerializeNationalIdentityNumberAsSimpleString() throws IOException {
        var request = new CreateUserRequest(new NationalIdentityNumber(randomString()), null, null);
        var serialized = request.toString();
        var json = JsonConfig.mapFrom(serialized);
        assertThat(json.get(CreateUserRequest.NATIONAL_IDENTITY_NUMBER_FIELD), is(instanceOf(String.class)));
    }

    private void assertThatNationalIdentityNumberIsNotRepresentedInternallyAsOnlyString(
        CreateUserRequest deserialized) {
        assertThat(deserialized.getNin(), is(instanceOf(NationalIdentityNumber.class)));
    }
}