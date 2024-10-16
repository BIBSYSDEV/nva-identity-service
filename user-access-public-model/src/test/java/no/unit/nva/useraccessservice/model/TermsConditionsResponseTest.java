package no.unit.nva.useraccessservice.model;

import org.junit.jupiter.api.Test;

import static no.unit.nva.testutils.RandomDataGenerator.randomLocalDateTime;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.model.TermsConditionsResponse.TermsConditionsResponseBuilder.aTermsConditionsResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;


class TermsConditionsResponseTest {

    @Test
    public void toStringShouldContainAllFields() {
        var id = randomUri();
        var validFrom = randomLocalDateTime();
        var agreement = randomString();

        var response = aTermsConditionsResponse()
            .withId(id)
            .withValidFrom(validFrom)
            .withAgreement(agreement)
            .build();

        assertThat(response.toString(), containsString(id.toString()));
        assertThat(response.toString(), containsString(validFrom.toString()));
        assertThat(response.toString(), containsString(agreement));
    }

}