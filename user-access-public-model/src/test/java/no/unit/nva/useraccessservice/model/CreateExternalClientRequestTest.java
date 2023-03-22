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
        var owner = randomString();
        var customerUri = randomCristinOrgId();
        var cristinUri = randomCristinOrgId();
        var scopes = List.of(randomString());

        var request =
            CreateExternalClientRequest.newBuilder()
                .withClientName(clientName)
                .withCustomerUri(customerUri)
                .withCristinUri(cristinUri)
                .withOwner(owner)
                .withScopes(scopes)
                .build();

        assertThat(request.toString(), containsString(clientName));
        assertThat(request.toString(), containsString(customerUri.toString()));
        assertThat(request.toString(), containsString(cristinUri.toString()));
        assertThat(request.toString(), containsString(owner));
        assertThat(request.toString(), containsString(scopes.get(0)));
    }

    @Test
    public void gettersShouldReturnSameValuesGivenToSetters() {
        var clientName = randomString();
        var owner = randomString();
        var customerUri = randomCristinOrgId();
        var cristinUri = randomCristinOrgId();
        var scopes = List.of(randomString());

        var request = new CreateExternalClientRequest();

        request.setClientName(clientName);
        request.setOwner(owner);
        request.setCustomerUri(customerUri);
        request.setCristinUri(cristinUri);
        request.setScopes(scopes);

        assertThat(request.getClientName(), is(equalTo(clientName)));
        assertThat(request.getCustomerUri(), is(equalTo(customerUri)));
        assertThat(request.getCristinUri(), is(equalTo(cristinUri)));
        assertThat(request.getOwner(), is(equalTo(owner)));
        assertThat(request.getScopes(), is(equalTo(scopes)));
    }
}