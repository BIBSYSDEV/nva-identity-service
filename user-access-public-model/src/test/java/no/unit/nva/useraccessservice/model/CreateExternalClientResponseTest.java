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
import no.unit.nva.RandomUserDataGenerator;
import no.unit.nva.testutils.RandomDataGenerator;
import org.junit.jupiter.api.Test;

public class CreateExternalClientResponseTest {


    @Test
    public void shouldHaveAnEmptyConstructor() {
        new CreateExternalClientResponse();
    }

    @Test
    public void toStringShouldContainAllFields() throws MalformedURLException {
        var id = randomString();
        var secret = randomString();
        var clientUrl = randomUri().toURL().toString();
        var customer = randomCristinOrgId();
        var scopes = List.of(randomString());

        var response = new CreateExternalClientResponse(id, secret, clientUrl, customer, scopes);

        assertThat(response.toString(), containsString(id));
        assertThat(response.toString(), containsString(secret));
        assertThat(response.toString(), containsString(clientUrl));
        assertThat(response.toString(), containsString(customer.toString()));
        assertThat(response.toString(), containsString(scopes.get(0)));
    }

    @Test
    public void gettersShouldReturnCorrectValues() {
        var id = randomString();
        var secret = randomString();
        var clientUrl = randomString();
        var customer = randomCristinOrgId();
        var scopes = List.of(randomString());

        var response = new CreateExternalClientResponse(id, secret, clientUrl, customer, scopes);

        assertThat(response.getClientId(), is(equalTo(id)));
        assertThat(response.getClientSecret(), is(equalTo(secret)));
        assertThat(response.getClientUrl(), is(equalTo(clientUrl)));
        assertThat(response.getCustomer(), is(equalTo(customer)));
        assertThat(response.getScopes(), is(equalTo(scopes)));
    }

}