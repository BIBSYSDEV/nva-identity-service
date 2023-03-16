package no.unit.nva.useraccessservice.model;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import java.net.MalformedURLException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class GetExternalClientResponseTest {


    @Test
    public void shouldHaveAnEmptyConstructor() {
        new CreateExternalClientResponse();
    }

    @Test
    public void toStringShouldContainAllFields() throws MalformedURLException {
        var clientId = randomString();
        var customer = randomCristinOrgId();

        var response = new GetExternalClientResponse(clientId, customer);

        assertThat(response.toString(), containsString(clientId));
        assertThat(response.toString(), containsString(customer.toString()));
    }

    @Test
    public void gettersShouldReturnCorrectValues() {
        var clientId = randomString();
        var customer = randomCristinOrgId();

        var response = new GetExternalClientResponse(clientId, customer);

        assertThat(response.getClientId(), is(equalTo(clientId)));
        assertThat(response.getCustomer(), is(equalTo(customer)));
    }

}