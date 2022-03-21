package no.unit.nva.cognito;

import static nva.commons.core.attempt.Try.attempt;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.accessrights.AccessRight;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.JacocoGenerated;

public class CustomerAccessRight {

    public static final String AT = "@";
    public static final String ACCESS_RIGHT_SEPARATOR = ",";
    private final AccessRight accessRight;
    private final CustomerDto customer;

    public CustomerAccessRight(CustomerDto customerDto,
                               AccessRight accessRight) {
        this.accessRight = accessRight;
        this.customer = customerDto;
    }

    public static List<CustomerAccessRight> fromUser(UserDto user, CustomerService customerService) {
        var customer =
            attempt(() -> customerService.getCustomer(user.getInstitution())).toOptional();

        return customer.isPresent()
                   ? createAccessRightsForExistingCustomer(user, customer.orElseThrow())
                   : customerDoesNotExist();
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
        return asStrings().stream().collect(Collectors.joining(ACCESS_RIGHT_SEPARATOR));
    }

    private static List<CustomerAccessRight> customerDoesNotExist() {
        return Collections.emptyList();
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
