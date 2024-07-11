package no.unit.nva.handlers;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.stream.Collectors;
import no.unit.nva.database.IdentityService;
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
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) {
        identityService.listAllUsers().stream()
            .filter(this::hasDeprecatedRole)
            .forEach(this::removeDeprecatedRoles);
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpURLConnection.HTTP_OK;
    }
    
    private boolean hasDeprecatedRole(UserDto user) {
        return user.getRoles().stream().map(RoleDto::getRoleName).anyMatch(RoleName::isDeprecated);
    }

    private void removeDeprecatedRoles(UserDto user) {
        attempt(() -> updateRolesForUser(user)).orElseThrow();
        logger.info("User roles has been updated: {}", user.getUsername());
    }

    private UserDto updateRolesForUser(UserDto user) throws NotFoundException {
        var rolesToKeep = user.getRoles().stream().filter(RoleDto::isNotDeprecated).collect(Collectors.toSet());
        user.setRoles(rolesToKeep);
        identityService.updateUser(user);
        return user;
    }
}
