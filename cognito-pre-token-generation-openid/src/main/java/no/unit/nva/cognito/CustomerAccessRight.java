package no.unit.nva.cognito;

import static no.unit.nva.cognito.CognitoClaims.AT;
import static no.unit.nva.cognito.CognitoClaims.ELEMENTS_DELIMITER;
import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.JacocoGenerated;

public class CustomerAccessRight {

    private final AccessRight accessRight;
    private final CustomerDto customer;

    public CustomerAccessRight(CustomerDto customerDto,
                               AccessRight accessRight) {
        this.accessRight = accessRight;
        this.customer = customerDto;
    }

    public static List<CustomerAccessRight> fromUser(UserDto user, CustomerService customerService) {
        var customer =
            attempt(() -> customerService.getCustomer(user.getInstitution())).orElseThrow();
        return createAccessRightsForExistingCustomer(user, customer);
    }

    public List<String> asStrings() {
        return List.of(accessRightWithCustomerId(), accessRightWithCustomerCristinId());
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
        if (!(o instanceof CustomerAccessRight)) {
            return false;
        }
        CustomerAccessRight that = (CustomerAccessRight) o;
        return accessRight == that.accessRight && Objects.equals(customer, that.customer);
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return asStrings().stream().collect(Collectors.joining(ELEMENTS_DELIMITER));
    }

    private static List<CustomerAccessRight> createAccessRightsForExistingCustomer(UserDto user, CustomerDto customer) {
        return user.getAccessRights()
            .stream()
            .map(AccessRight::fromString)
            .map(accessRight -> new CustomerAccessRight(customer, accessRight))
            .collect(Collectors.toList());
    }

    private String accessRightWithCustomerCristinId() {

        return accessRight.toString() + AT + customer.getCristinId();
    }

    private String accessRightWithCustomerId() {
        return accessRight.toString() + AT + customer.getId();
    }
}
