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
        var actingUser = randomString();
        var customerUri = randomCristinOrgId();
        var cristinOrgUri = randomCristinOrgId();
        var scopes = List.of(randomString());

        var request =
            CreateExternalClientRequest.newBuilder()
                .withClientName(clientName)
                .withCustomerUri(customerUri)
                .withCristinOrgUri(cristinOrgUri)
                .withActingUser(actingUser)
                .withScopes(scopes)
                .build();

        assertThat(request.toString(), containsString(clientName));
        assertThat(request.toString(), containsString(customerUri.toString()));
        assertThat(request.toString(), containsString(cristinOrgUri.toString()));
        assertThat(request.toString(), containsString(actingUser));
        assertThat(request.toString(), containsString(scopes.get(0)));
    }

    @Test
    public void gettersShouldReturnSameValuesGivenToSetters() {
        var clientName = randomString();
        var actingUser = randomString();
        var customerUri = randomCristinOrgId();
        var cristinOrgUri = randomCristinOrgId();
        var scopes = List.of(randomString());

        var request = new CreateExternalClientRequest();

        request.setClientName(clientName);
        request.setActingUser(actingUser);
        request.setCustomerUri(customerUri);
        request.setCristinOrgUri(cristinOrgUri);
        request.setScopes(scopes);

        assertThat(request.getClientName(), is(equalTo(clientName)));
        assertThat(request.getCustomerUri(), is(equalTo(customerUri)));
        assertThat(request.getCristinOrgUri(), is(equalTo(cristinOrgUri)));
        assertThat(request.getActingUser(), is(equalTo(actingUser)));
        assertThat(request.getScopes(), is(equalTo(scopes)));
    }
}