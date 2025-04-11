package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.util.Optional;

import static java.util.function.Predicate.not;

public class GetRoleHandler extends ApiGatewayHandler<Void, RoleDto> {

    public static final String ROLE_PATH_PARAMETER = "role";

    private final IdentityService databaseService;

    /**
     * Default constructor used by AWS Lambda.
     */
    @JacocoGenerated
    public GetRoleHandler() {
        this(new IdentityServiceImpl(), new Environment());
    }

    public GetRoleHandler(IdentityService databaseService, Environment environment) {
        super(Void.class, environment);
        this.databaseService = databaseService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    public RoleDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws NotFoundException {
        var roleName = roleNameThatIsNotNullOrBlank(requestInfo);

        RoleDto searchObject = RoleDto.newBuilder().withRoleName(roleName).build();
        return databaseService.getRole(searchObject);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, RoleDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    private RoleName roleNameThatIsNotNullOrBlank(RequestInfo requestInfo) {
        return Optional.ofNullable(requestInfo.getPathParameters())
            .map(pathParams -> pathParams.get(ROLE_PATH_PARAMETER))
            .filter(not(String::isBlank))
            .map(RoleName::fromValue)
            .orElseThrow();
    }
}
