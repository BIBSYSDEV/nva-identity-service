package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.IdentityService;
import no.unit.nva.handlers.data.DefaultRoleSource;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

import static nva.commons.core.attempt.Try.attempt;

public class IdentityServiceMigrateCuratorHandler extends ApiGatewayHandler<Void, Void> {

    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceMigrateCuratorHandler.class);

    private final IdentityService identityService;

    @JacocoGenerated
    public IdentityServiceMigrateCuratorHandler() {
        this(IdentityService.defaultIdentityService());
    }

    public IdentityServiceMigrateCuratorHandler(IdentityService identityService) {
        super(Void.class);
        this.identityService = identityService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context)
            throws InvalidInputException, NotFoundException {
        identityService.updateRole(DefaultRoleSource.PUBLISHING_CURATOR_ROLE);
        identityService.listAllUsers().stream()
                .filter(this::hasPublishingCuratorRole)
                .forEach(this::updatePublishingCuratorAccessRights);
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpURLConnection.HTTP_OK;
    }

    private boolean hasPublishingCuratorRole(UserDto user) {
        return user.getRoles().stream().map(RoleDto::getRoleName).anyMatch(RoleName.PUBLISHING_CURATOR::equals);
    }

    private void updatePublishingCuratorAccessRights(UserDto user) {
        attempt(() -> updateRolesForUser(user)).orElseThrow();
        logger.info("User roles has been updated: {}", user.getUsername());
    }

    private UserDto updateRolesForUser(UserDto user) throws NotFoundException {
        var roles = user.getRoles();
        var roleToUpdate = roles.stream().filter(this::isPublishingCurator).findFirst().orElseThrow();
        roles.remove(roleToUpdate);
        roles.add(DefaultRoleSource.PUBLISHING_CURATOR_ROLE);
        var updatedUser = user.copy().withRoles(roles).build();
        identityService.updateUser(updatedUser);
        return user;
    }

    private boolean isPublishingCurator(RoleDto role) {
        return RoleName.PUBLISHING_CURATOR.equals(role.getRoleName());
    }
}
