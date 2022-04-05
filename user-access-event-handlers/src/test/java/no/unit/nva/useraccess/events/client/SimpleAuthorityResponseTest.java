package no.unit.nva.useraccess.events.client;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimpleAuthorityResponseTest {

    @Test
    void shouldSerializeToJson() {
        var sample = new SimpleAuthorityResponse();
        sample.setId(randomUri());
        sample.setOrganizationIds(List.of(randomUri()));
        var json = sample.toString();
        var deserialized = SimpleAuthorityResponse.fromJson(json);
        assertThat(deserialized, is(equalTo(sample)));
    }
}