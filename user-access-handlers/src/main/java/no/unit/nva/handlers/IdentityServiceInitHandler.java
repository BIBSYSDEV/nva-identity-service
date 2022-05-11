package no.unit.nva.handlers;

import static nva.commons.apigateway.AccessRight.ADMINISTRATE_APPLICATION;
import static nva.commons.apigateway.AccessRight.APPROVE_DOI_REQUEST;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_PROJECTS;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_USERS;
import static nva.commons.apigateway.AccessRight.READ_DOI_REQUEST;
import static nva.commons.apigateway.AccessRight.REJECT_DOI_REQUEST;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.database.IdentityService;
import no.unit.nva.handlers.models.RoleList;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityServiceInitHandler extends ApiGatewayHandler<Void, RoleList> {

    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceInitHandler.class);
    private final IdentityService identityService;

    @JacocoGenerated
    public IdentityServiceInitHandler() {
        this(IdentityService.defaultIdentityService());
    }

    public IdentityServiceInitHandler(IdentityService identityService) {
        super(Void.class);
        this.identityService = identityService;
    }

    @Override
    protected RoleList processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var defaultRoles = createDefaultRoles()
            .stream()
            .map(attempt(this::addRole))
            .map(attempt -> attempt.toOptional(fail -> logError(fail.getException())))
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());

        return new RoleList(defaultRoles);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, RoleList output) {
        return HttpURLConnection.HTTP_OK;
    }

    private void logError(Exception exception) {
        logger.warn(exception.getMessage());
    }

    private RoleDto addRole(RoleDto role) throws ConflictException, InvalidInputException {
        logger.info("Adding role:{}", role);
        identityService.addRole(role);
        return role;
    }

    private List<RoleDto> createDefaultRoles() {
        var creator = RoleDto.newBuilder().withRoleName("Creator").build();
        var curator = RoleDto.newBuilder().withRoleName("Curator")
            .withAccessRights(
                List.of(APPROVE_DOI_REQUEST, REJECT_DOI_REQUEST, READ_DOI_REQUEST, EDIT_OWN_INSTITUTION_RESOURCES))
            .build();
        var institutionAdmin = RoleDto.newBuilder().withRoleName("Institution-admin")
            .withAccessRights(List.of(EDIT_OWN_INSTITUTION_RESOURCES,
                                      EDIT_OWN_INSTITUTION_PROJECTS,
                                      EDIT_OWN_INSTITUTION_USERS,
                                      EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW))
            .build();
        var applicationAdmin = RoleDto.newBuilder().withRoleName("App-admin")
            .withAccessRights(List.of(ADMINISTRATE_APPLICATION))
            .build();

        var editor = RoleDto.newBuilder().withRoleName("Editor")
            .withAccessRights(List.of(EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW))
            .build();

        return List.of(creator, curator, institutionAdmin, applicationAdmin, editor);
    }
}
