package no.unit.nva.useraccess.events.service;

import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccess.events.client.BareProxyClient;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;

import java.net.URI;
import java.util.UUID;

import static nva.commons.core.attempt.Try.attempt;

public class UserMigrationServiceImpl implements UserMigrationService {

    private final CustomerService customerService;
    private final BareProxyClient bareProxyClient;

    public UserMigrationServiceImpl(CustomerService customerService, BareProxyClient bareProxyClient) {
        this.customerService = customerService;
        this.bareProxyClient = bareProxyClient;
    }

    @Override
    public UserDto migrateUser(UserDto user) {
        var customerIdentifier = getCustomerIdentifier(user);
        var organizationId = getOrganizationId(customerIdentifier);

        removeOldPatternOrganizationIds(user.getUsername());
        resetViewingScope(user, organizationId);

        return user;
    }

    private void removeOldPatternOrganizationIds(String username) {
        var authority = bareProxyClient.getAuthorityByFeideId(username);
        if (authority.isPresent()) {
            var systemControlNumber = authority.get().getSystemControlNumber();
            var organizationIds = authority.get().getOrganizationIds();
            organizationIds.stream()
                    .filter(ViewingScope::isNotValidOrganizationId)
                    .forEach(uri -> deleteFromAuthority(systemControlNumber, uri));
        }
    }

    private void deleteFromAuthority(String systemControlNumber, URI organizationId) {
        bareProxyClient.deleteAuthorityOrganizationId(systemControlNumber, organizationId);
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
