package no.unit.nva.useraccessservice.model;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import java.net.MalformedURLException;
import org.junit.jupiter.api.Test;

public class GetExternalClientResponseTest {


    @Test
    public void shouldHaveAnEmptyConstructor() {
        new CreateExternalClientResponse();
    }

    @Test
    public void toStringShouldContainAllFields() {
        var clientId = randomString();
        var customerUri = randomCristinOrgId();
        var cristinUri = randomCristinOrgId();
        var owner = randomString();

        var response = new GetExternalClientResponse(clientId, customerUri, cristinUri, owner);

        assertThat(response.toString(), containsString(clientId));
        assertThat(response.toString(), containsString(customerUri.toString()));
        assertThat(response.toString(), containsString(cristinUri.toString()));
        assertThat(response.toString(), containsString(owner));
    }

    @Test
    public void gettersShouldReturnCorrectValues() {
        var clientId = randomString();
        var customerUri = randomCristinOrgId();
        var cristinUri = randomCristinOrgId();
        var owner = randomString();


        var response = new GetExternalClientResponse(clientId, customerUri, cristinUri, owner);

        assertThat(response.getClientId(), is(equalTo(clientId)));
        assertThat(response.getCustomerUri(), is(equalTo(customerUri)));
        assertThat(response.getCristinUri(), is(equalTo(cristinUri)));
        assertThat(response.getOwner(), is(equalTo(owner)));
    }

}