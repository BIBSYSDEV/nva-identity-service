package no.unit.nva.useraccess.events.service;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.ViewingScope;
import nva.commons.core.exceptions.ExceptionUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserMigrationServiceImpl implements UserMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(UserMigrationServiceImpl.class);
    private final CustomerService customerService;

    public UserMigrationServiceImpl(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public UserDto migrateUser(UserDto user) {
        logger.trace("Updating user:{}", user.getUsername());
        resetViewingScope(user);
        return user;
    }

    private void resetViewingScope(UserDto user) {
        getCustomerIdentifier(user)
            .map(this::getOrganizationId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .ifPresent(orgId -> resetViewingScope(user, orgId));
    }

    private void resetViewingScope(UserDto user, URI organizationId) {
        user.setViewingScope(ViewingScope.defaultViewingScope(organizationId));
    }

    private Optional<UUID> getCustomerIdentifier(UserDto user) {
        return attempt(user::getInstitution)
            .map(UriWrapper::fromUri)
            .map(UriWrapper::getLastPathElement)
            .map(UUID::fromString)
            .toOptional(f -> logInvalidInstitutionUriAndReturnEmpty(f.getException(), user));
    }

    private void logInvalidInstitutionUriAndReturnEmpty(Exception exception, UserDto user) {
        logger.error("Customer Id {} is invalid for user {}", user.getInstitution(), user.getUsername());
        logger.error(ExceptionUtils.stackTraceInSingleLine(exception));
    }

    private Optional<URI> getOrganizationId(UUID customerIdentifier) {
        return attempt(() -> customerService.getCustomer(customerIdentifier))
            .map(CustomerDto::getCristinId)
            .toOptional();
    }
}
