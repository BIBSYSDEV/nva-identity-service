package no.unit.nva.useraccessservice.usercreation;

import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserCreationContextTest {

    private static final String feideIdentifier = randomString();
    private static final Set<CustomerDto> customers = Collections.emptySet();
    private static final Person person = new Person(randomUri(), randomString(), randomString(), randomString(),
            Collections.emptyList());
    private static final String EMPTY_STRING = "";

    @Test
    void shouldThrowExceptionWhenCreatingInstanceWithNullPerson() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserCreationContext(null, customers, feideIdentifier));
    }

    @Test
    void shouldThrowExceptionWhenCreatingInstanceWithNullCustomers() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserCreationContext(person, null, feideIdentifier));
    }

    @Test
    void shouldThrowExceptionWhenCreatingInstanceWithEmptyFeideIdentifier() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserCreationContext(person, customers, EMPTY_STRING));
    }
}
