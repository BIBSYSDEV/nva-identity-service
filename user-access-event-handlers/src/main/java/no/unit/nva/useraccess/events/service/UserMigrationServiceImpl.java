package no.unit.nva.useraccess.events.service;

import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessmanagement.dao.UserDb;
import no.unit.nva.useraccessmanagement.model.ViewingScope;

import java.net.URI;
import java.util.UUID;

import static nva.commons.core.attempt.Try.attempt;

public class UserMigrationServiceImpl implements UserMigrationService {

    private final CustomerService customerService;

    public UserMigrationServiceImpl(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public UserDb migrateUser(UserDb user) {
        var customerIdentifier = getCustomerIdentifier(user);
        var organizationId = getOrganizationId(customerIdentifier);

        //TODO: update organizationIds in Bare

        resetViewingScope(user, organizationId);

        return user;
    }

    private UUID getCustomerIdentifier(UserDb user) {
        return UUID.fromString(user.getInstitution());
    }

    private void resetViewingScope(UserDb user, URI organizationId) {
        user.setViewingScope(ViewingScope.defaultViewingScope(organizationId));
    }

    private URI getOrganizationId(UUID customerIdentifier) {
        var customer = attempt(() -> customerService.getCustomer(customerIdentifier)).orElseThrow();
        return URI.create(customer.getCristinId());
    }
}
