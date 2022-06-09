package no.unit.nva.cognito;

import static no.unit.nva.cognito.CognitoClaims.AT;
import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.JacocoGenerated;

public class UserAccessRightForCustomer {

    private final AccessRight accessRight;
    private final CustomerDto customer;

    public UserAccessRightForCustomer(CustomerDto customerDto,
                                      AccessRight accessRight) {
        this.accessRight = accessRight;
        this.customer = customerDto;
    }

    public static List<UserAccessRightForCustomer> fromUser(UserDto user, CustomerService customerService) {
        var customer =
            attempt(() -> customerService.getCustomer(user.getInstitution())).orElseThrow();
        return createAccessRightsForExistingCustomer(user, customer);
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
        if (!(o instanceof UserAccessRightForCustomer)) {
            return false;
        }
        UserAccessRightForCustomer that = (UserAccessRightForCustomer) o;
        return accessRight == that.accessRight && Objects.equals(customer, that.customer);
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return accessRightWithCustomerId();
    }

    private static List<UserAccessRightForCustomer> createAccessRightsForExistingCustomer(UserDto user,
                                                                                          CustomerDto customer) {
        return user.getAccessRights()
            .stream()
            .map(accessRight -> new UserAccessRightForCustomer(customer, accessRight))
            .collect(Collectors.toList());
    }

    private String accessRightWithCustomerId() {
        return accessRight.toString() + AT + customer.getId();
    }
}
