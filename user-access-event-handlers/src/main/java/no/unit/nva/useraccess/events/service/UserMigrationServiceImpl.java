package no.unit.nva.useraccess.events.service;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.UUID;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccess.events.client.BareProxyClient;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserMigrationServiceImpl implements UserMigrationService {

    private final CustomerService customerService;
    private final BareProxyClient bareProxyClient;
    private static final Logger logger = LoggerFactory.getLogger(UserMigrationServiceImpl.class);

    public UserMigrationServiceImpl(CustomerService customerService, BareProxyClient bareProxyClient) {
        this.customerService = customerService;
        this.bareProxyClient = bareProxyClient;
    }

    @Override
    public UserDto migrateUser(UserDto user) {
        var customerIdentifier = getCustomerIdentifier(user);
        logger.trace("Updating user:{}", user.getUsername());
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
        return attempt(user::getInstitution)
            .map(UriWrapper::new)
            .map(UriWrapper::getFilename)
            .map(UUID::fromString)
            .orElseThrow(f -> logInvalidInstitutionUriAndThrowException(f.getException(), user));
    }

    private RuntimeException logInvalidInstitutionUriAndThrowException(Exception exception, UserDto user) {
        logger.error("Customer Id {} is invalid for user {}", user.getInstitution(), user.getUsername());
        if (exception instanceof RuntimeException) {
            return (RuntimeException) exception;
        } else {
            return new RuntimeException(exception);
        }
    }

    private void resetViewingScope(UserDto user, URI organizationId) {
        user.setViewingScope(ViewingScope.defaultViewingScope(organizationId));
    }

    private URI getOrganizationId(UUID customerIdentifier) {
        var customer = attempt(() -> customerService.getCustomer(customerIdentifier)).orElseThrow();
        return URI.create(customer.getCristinId());
    }
}
