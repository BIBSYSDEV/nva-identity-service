package no.unit.nva.useraccessservice.usercreation.cristin.person;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.List;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import org.junit.jupiter.api.Test;

class CristinPersonResponseTest {

    @Test
    void builderShouldFillInAllFields() {
        var response = CristinPersonResponse.builder()
            .withCristinId(randomUri())
            .withAffiliations(randomCristinAffiliations())
            .withFirstName(randomString())
            .withLastName(randomString())
            .withNin(new NationalIdentityNumber(randomString()))
            .withCristinId(randomUri())
            .build();
        assertThat(response, doesNotHaveEmptyValues());
    }

    private List<CristinAffiliation> randomCristinAffiliations() {
        var affiliation = CristinAffiliation.builder()
            .withActive(randomBoolean())
            .withOrganization(randomUri())
            .build();
        return List.of(affiliation);
    }
}