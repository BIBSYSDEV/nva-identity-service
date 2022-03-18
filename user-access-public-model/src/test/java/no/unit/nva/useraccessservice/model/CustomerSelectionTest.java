package no.unit.nva.useraccessservice.model;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.io.IOException;
import no.unit.nva.identityservice.json.JsonConfig;
import org.junit.jupiter.api.Test;

class CustomerSelectionTest {

    @Test
    void shouldReturnObjectWithInputUri() {
        var input = randomUri();
        var customerSelection = CustomerSelection.fromCustomerId(input);
        assertThat(customerSelection.getCustomerId(), is(equalTo(input)));
    }

    @Test
    void toStringReturnsJson() throws IOException {
        var customerSelection = CustomerSelection.fromCustomerId(randomUri());
        var json = customerSelection.toString();
        var deserialized = JsonConfig.beanFrom(CustomerSelection.class, json);
        assertThat(deserialized.getCustomerId(), is(equalTo(customerSelection.getCustomerId())));
    }
}