package no.unit.nva.useraccessservice.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import no.unit.nva.testutils.RandomDataGenerator;
import org.junit.jupiter.api.Test;

public class CreateExternalUserResponseTest {


    @Test
    public void shouldHaveAnEmptyConstructor() {
        new CreateExternalUserResponse();
    }

    @Test
    public void toStringShouldContainAllFields() {
        var id = RandomDataGenerator.randomString();
        var secret = RandomDataGenerator.randomString();
        var url = RandomDataGenerator.randomString();

        var response = new CreateExternalUserResponse(id, secret, url);

        assertThat(response.toString(), containsString(id));
        assertThat(response.toString(), containsString(secret));
        assertThat(response.toString(), containsString(url));
    }

    @Test
    public void gettersShouldReturnCorrectValues() {
        var id = RandomDataGenerator.randomString();
        var secret = RandomDataGenerator.randomString();
        var url = RandomDataGenerator.randomString();

        var response = new CreateExternalUserResponse(id, secret, url);

        assertThat(response.getClientId(), is(equalTo(id)));
        assertThat(response.getClientSecret(), is(equalTo(secret)));
        assertThat(response.getClientUrl(), is(equalTo(url)));
    }

}