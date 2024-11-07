package no.unit.nva.useraccessservice.usercreation;

import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import nva.commons.core.StringUtils;

import java.util.Objects;
import java.util.Set;

public class UserCreationContext {
    private final Person person;
    private final Set<CustomerDto> customers;
    private final String feideIdentifier;

    public UserCreationContext(Person person, Set<CustomerDto> customers, String feideIdentifier) {
        assertValidState(person, customers, feideIdentifier);

        this.person = person;
        this.customers = customers;
        this.feideIdentifier = feideIdentifier;
    }

    public UserCreationContext(Person person, Set<CustomerDto> customers) {
        this(person, customers, null);
    }

    private void assertValidState(Person person, Set<CustomerDto> customers, String authenticatedFeideIdentifier) {
        if (Objects.isNull(person)) {
            throw new IllegalArgumentException("person is null");
        }
        if (Objects.isNull(customers)) {
            throw new IllegalArgumentException("customers is null");
        }
        if (authenticatedFeideIdentifier != null && StringUtils.isEmpty(authenticatedFeideIdentifier)) {
            throw new IllegalArgumentException("authenticatedFeideIdentifier must be null or not empty");
        }
    }

    public Person getPerson() {
        return person;
    }

    public Set<CustomerDto> getCustomers() {
        return customers;
    }

    public String getFeideIdentifier() {
        return feideIdentifier;
    }
}
