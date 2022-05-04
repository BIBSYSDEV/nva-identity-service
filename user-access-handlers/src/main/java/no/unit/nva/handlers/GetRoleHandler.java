package no.unit.nva.handlers;

import static java.util.function.Predicate.not;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Optional;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class GetRoleHandler extends ApiGatewayHandler<Void, RoleDto> {

    public static final String EMPTY_ROLE_NAME = "Role-name cannot be empty";
    public static final String ROLE_PATH_PARAMETER = "role";

    private final IdentityService databaseService;

    /**
     * Default constructor used by AWS Lambda.
     */
    @JacocoGenerated
    public GetRoleHandler() {
        this(new IdentityServiceImpl());
    }

    public GetRoleHandler(IdentityService databaseService) {
        super(Void.class);
        this.databaseService = databaseService;
    }

    @Override
    public RoleDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws BadRequestException, NotFoundException {
        String roleName = roleNameThatIsNotNullOrBlank(requestInfo);

        RoleDto searchObject = RoleDto.newBuilder().withRoleName(roleName).build();
        return databaseService.getRole(searchObject);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, RoleDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    private String roleNameThatIsNotNullOrBlank(RequestInfo requestInfo) throws BadRequestException {
        return Optional.ofNullable(requestInfo.getPathParameters())
            .map(pathParams -> pathParams.get(ROLE_PATH_PARAMETER))
            .filter(not(String::isBlank))
            .orElseThrow(() -> new BadRequestException(EMPTY_ROLE_NAME));
    }
}
