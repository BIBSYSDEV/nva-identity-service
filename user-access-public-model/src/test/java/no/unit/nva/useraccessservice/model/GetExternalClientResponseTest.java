package no.unit.nva.useraccessservice.model;

import org.junit.jupiter.api.Test;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;

public class GetExternalClientResponseTest {


    @Test
    public void shouldHaveAnEmptyConstructor() {
        new CreateExternalClientResponse();
    }

    @Test
    public void toStringShouldContainAllFields() {
        var clientId = randomString();
        var customerUri = randomCristinOrgId();
        var cristinOrgUri = randomCristinOrgId();
        var actingUser = randomString();

        var response = new GetExternalClientResponse(clientId, customerUri, cristinOrgUri, actingUser);

        assertThat(response.toString(), containsString(clientId));
        assertThat(response.toString(), containsString(customerUri.toString()));
        assertThat(response.toString(), containsString(cristinOrgUri.toString()));
        assertThat(response.toString(), containsString(actingUser));
    }

    @Test
    public void gettersShouldReturnCorrectValues() {
        var clientId = randomString();
        var customerUri = randomCristinOrgId();
        var cristinOrgUri = randomCristinOrgId();
        var actingUser = randomString();


        var response = new GetExternalClientResponse(clientId, customerUri, cristinOrgUri, actingUser);

        assertThat(response.getClientId(), is(equalTo(clientId)));
        assertThat(response.getCustomerUri(), is(equalTo(customerUri)));
        assertThat(response.getCristinOrgUri(), is(equalTo(cristinOrgUri)));
        assertThat(response.getActingUser(), is(equalTo(actingUser)));
    }

}