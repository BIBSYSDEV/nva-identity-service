package no.unit.nva.useraccessservice.usercreation.cristin.person;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.junit.jupiter.api.Test;

class NameValueTest {

    @Test
    void isFirstNameShouldReturnTrueWhenNameValueIsFirstName() {
        var name = NameValue.firstName(randomString());
        assertThat(name.isFirstName(), is(true));
        assertThat(name.isLastName(), is(false));
    }

    @Test
    void isLastNameShouldReturnTrueWhenNameValueIsLastName() {
        var name = NameValue.lastName(randomString());
        assertThat(name.isLastName(), is(true));
        assertThat(name.isFirstName(), is(false));
    }
}