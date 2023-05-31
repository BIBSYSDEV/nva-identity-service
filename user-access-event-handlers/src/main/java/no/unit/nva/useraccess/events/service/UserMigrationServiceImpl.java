package no.unit.nva.useraccess.events.service;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccess.events.client.BareProxyClient;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.ViewingScope;
import nva.commons.core.exceptions.ExceptionUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserMigrationServiceImpl implements UserMigrationService {

    public static final String CRISTIN_API_HOST = "api.cristin.no";
    public static final String UNDEFINED = "undefined";
    private static final Logger logger = LoggerFactory.getLogger(UserMigrationServiceImpl.class);
    private final CustomerService customerService;
    private final BareProxyClient bareProxyClient;

    public UserMigrationServiceImpl(CustomerService customerService, BareProxyClient bareProxyClient) {
        this.customerService = customerService;
        this.bareProxyClient = bareProxyClient;
    }

    @Override
    public UserDto migrateUser(UserDto user) {
        logger.trace("Updating user:{}", user.getUsername());
        resetViewingScope(user);
        removeOldPatternOrganizationIds(user.getUsername());
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

    private void removeOldPatternOrganizationIds(String username) {
        var authority = bareProxyClient.getAuthorityByFeideId(username);
        if (authority.isPresent()) {
            var systemControlNumber = authority.get().getSystemControlNumber();
            var organizationIds = authority.get().getOrganizationIds();
            organizationIds.stream()
                .filter(this::isUnwantedUri)
                .forEach(uri -> deleteFromAuthority(systemControlNumber, uri));
        }
    }

    private boolean isUnwantedUri(URI organizationId) {
        return CRISTIN_API_HOST.equals(organizationId.getHost())
               || UNDEFINED.equals(organizationId.toString());
    }

    private void deleteFromAuthority(String systemControlNumber, URI organizationId) {
        bareProxyClient.deleteAuthorityOrganizationId(systemControlNumber, organizationId);
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
