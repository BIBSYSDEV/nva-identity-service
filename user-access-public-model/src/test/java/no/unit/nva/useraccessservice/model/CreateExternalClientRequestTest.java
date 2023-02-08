package no.unit.nva.useraccessservice.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import no.unit.nva.testutils.RandomDataGenerator;
import org.junit.jupiter.api.Test;


public class CreateExternalClientRequestTest {

    @Test
    public void shouldHaveAnEmptyConstructor() {
        new CreateExternalClientRequest();
    }

    @Test
    public void toStringShouldContainAllFields() {
        var clientName = RandomDataGenerator.randomString();

        var request = new CreateExternalClientRequest(clientName);

        assertThat(request.toString(), containsString(clientName));
    }

    @Test
    public void gettersShouldReturnCorrectValues() {
        var clientName = RandomDataGenerator.randomString();

        var request = new CreateExternalClientRequest(clientName);

        assertThat(request.getClientName(), is(equalTo(clientName)));
    }
}