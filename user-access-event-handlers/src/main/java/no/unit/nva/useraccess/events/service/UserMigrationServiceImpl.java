package no.unit.nva.useraccess.events.service;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.UUID;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;

public class UserMigrationServiceImpl implements UserMigrationService {

    private final CustomerService customerService;

    public UserMigrationServiceImpl(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public UserDto migrateUser(UserDto user) {
        var customerIdentifier = getCustomerIdentifier(user);
        var organizationId = getOrganizationId(customerIdentifier);

        //TODO: update organizationIds in Bare

        resetViewingScope(user, organizationId);

        return user;
    }

    private UUID getCustomerIdentifier(UserDto user) {
        return UUID.fromString(user.getInstitution());
    }

    private void resetViewingScope(UserDto user, URI organizationId) {
        user.setViewingScope(ViewingScope.defaultViewingScope(organizationId));
    }

    private URI getOrganizationId(UUID customerIdentifier) {
        var customer = attempt(() -> customerService.getCustomer(customerIdentifier)).orElseThrow();
        return URI.create(customer.getCristinId());
    }
}
