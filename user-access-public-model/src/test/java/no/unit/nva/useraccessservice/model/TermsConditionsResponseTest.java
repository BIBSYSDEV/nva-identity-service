package no.unit.nva.useraccessservice.model;

import org.junit.jupiter.api.Test;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;


class TermsConditionsResponseTest {

    @Test
    public void toStringShouldContainAllFields() {
        var id = randomUri();

        var response = TermsConditionsResponse.builder()
            .withTermsConditionsUri(id)
            .build();

        assertThat(response.toString(), containsString(id.toString()));

    }

}