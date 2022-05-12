package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.URI;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import org.junit.jupiter.api.Test;

class PersonInformationTest {

    //TODO: solve the coverage problem with inheritance?
    @Test
    void shouldReturnPersonRegistryIdWhenCristinResponseExists() {
        var personInformation = new PersonInformationImpl(randomString(), randomString());
        URI personRegistryId = randomUri();
        CristinPersonResponse response = CristinPersonResponse.builder().withCristinId(personRegistryId).build();
        personInformation.setCristinPersonResponse(response);
        assertThat(personInformation.getPersonRegistryId().orElseThrow(), is(equalTo(personRegistryId)));
    }
}