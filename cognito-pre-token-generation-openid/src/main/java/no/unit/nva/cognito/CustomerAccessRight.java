package no.unit.nva.cognito;

import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.useraccessserivce.accessrights.AccessRight;
import nva.commons.core.paths.UriWrapper;

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
        var customer = customerService.getCustomer(user.getInstitution());
        return user.getAccessRights()
            .stream()
            .map(AccessRight::fromString)
            .map(accessRight -> new CustomerAccessRight(customer, accessRight))
            .collect(Collectors.toList());
    }

    public List<String> asStrings() {
        return List.of(accessRightWithCustomerId(), accessRightWithCustomerCristinId());
    }

    @Override
    public String toString() {
        return asStrings().stream().collect(Collectors.joining(ACCESS_RIGHT_SEPARATOR));
    }

    private String accessRightWithCustomerCristinId() {
        var cristinIdentifier = UriWrapper.fromUri(customer.getCristinId()).getLastPathElement();
        return accessRight.toString() + AT + cristinIdentifier;
    }

    private String accessRightWithCustomerId() {
        return accessRight.toString() + AT + customer.getIdentifier();
    }
}
