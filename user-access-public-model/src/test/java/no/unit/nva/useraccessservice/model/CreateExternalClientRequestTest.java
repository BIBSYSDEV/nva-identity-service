package no.unit.nva.useraccessservice.model;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import java.util.List;
import org.junit.jupiter.api.Test;


public class CreateExternalClientRequestTest {

    @Test
    public void shouldHaveAnEmptyConstructor() {
        new CreateExternalClientRequest();
    }

    @Test
    public void toStringShouldContainAllFields() {
        var clientName = randomString();
        var customer = randomCristinOrgId();
        var scopes = List.of(randomString());

        var request = new CreateExternalClientRequest(clientName, customer, scopes);

        assertThat(request.toString(), containsString(clientName));
        assertThat(request.toString(), containsString(customer.toString()));
        assertThat(request.toString(), containsString(scopes.get(0)));
    }

    @Test
    public void gettersShouldReturnCorrectValues() {
        var clientName = randomString();
        var customer = randomCristinOrgId();
        var scopes = List.of(randomString());

        var request = new CreateExternalClientRequest(clientName, customer, scopes);

        assertThat(request.getClientName(), is(equalTo(clientName)));
        assertThat(request.getCustomer(), is(equalTo(customer)));
        assertThat(request.getScopes(), is(equalTo(scopes)));
    }
}