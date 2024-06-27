package no.unit.nva.handlers;

import static no.unit.nva.handlers.data.DefaultRoleSource.NVI_CURATOR_ROLE;
import static no.unit.nva.useraccessservice.model.RoleName.DEPRECATED_NVI_CURATOR;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Set;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
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
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) {
        identityService.listAllUsers().stream()
            .filter(this::userIsLegacyNviCurator)
            .forEach(this::attemptUpdateRolesForUser);

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpURLConnection.HTTP_OK;
    }
    
    private boolean userIsLegacyNviCurator(UserDto user) {
        return user.getRoles().stream().map(RoleDto::getRoleName).anyMatch(DEPRECATED_NVI_CURATOR::equals);
    }

    private UserDto attemptUpdateRolesForUser(UserDto user) {
        return attempt(() -> updateRolesForUser(user)).orElseThrow();
    }

    private UserDto updateRolesForUser(UserDto user) throws NotFoundException {
        logger.info("Updating nvi roles for {}", user.getUsername());
        var roles = user.getRoles();
        user.setRoles(updateRoleSet(roles));

        identityService.updateUser(user);
        return user;
    }

    private Set<RoleDto> updateRoleSet(Set<RoleDto> roleSet) {
        roleSet.add(NVI_CURATOR_ROLE);
        roleSet.removeIf(role -> DEPRECATED_NVI_CURATOR.equals(role.getRoleName()));
        return roleSet;
    }
}
