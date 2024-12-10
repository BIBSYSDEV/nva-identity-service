package no.unit.nva.cognito;

import static no.unit.nva.cognito.CognitoClaims.AT;

import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.lang.Boolean.FALSE;

public class UserAccessRightForCustomer {

    private final AccessRight accessRight;
    private final CustomerDto customer;

    public UserAccessRightForCustomer(CustomerDto customerDto,
                                      AccessRight accessRight) {
        this.accessRight = accessRight;
        this.customer = customerDto;
    }

    public static List<UserAccessRightForCustomer> fromUser(UserDto user, Set<CustomerDto> customers,
                                                            Boolean hasAcceptedTerms) {
        if (FALSE.equals(hasAcceptedTerms)) {
            return List.of();
        }

        var customer = customers.stream()
            .filter(candidateCustomer -> candidateCustomer.getId().equals(user.getInstitution()))
            .collect(SingletonCollector.collect());
        return createAccessRightsForExistingCustomer(user, customer);
    }

    private static List<UserAccessRightForCustomer> createAccessRightsForExistingCustomer(UserDto user,
                                                                                          CustomerDto customer) {
        return user.getAccessRights()
            .stream()
            .map(accessRight -> new UserAccessRightForCustomer(customer, accessRight))
            .toList();
    }

    public CustomerDto getCustomer() {
        return customer;
    }

    public AccessRight getAccessRight() {
        return accessRight;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(accessRight, customer);
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserAccessRightForCustomer that)) {
            return false;
        }
        return accessRight == that.accessRight && Objects.equals(customer, that.customer);
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return accessRightWithCustomerId();
    }

    private String accessRightWithCustomerId() {
        return accessRight.toPersistedString() + AT + customer.getId();
    }
}
