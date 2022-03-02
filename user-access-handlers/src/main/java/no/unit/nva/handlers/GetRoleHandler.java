package no.unit.nva.handlers;

import static java.util.function.Predicate.not;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.util.Optional;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.BadRequestException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.apigatewayv2.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class GetRoleHandler extends ApiGatewayHandlerV2<Void, RoleDto> {

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
        super();
        this.databaseService = databaseService;
    }

    @Override
    public RoleDto processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context)
        throws ApiGatewayException {
        String roleName = roleNameThatIsNotNullOrBlank(requestInfo);

        RoleDto searchObject = RoleDto.newBuilder().withRoleName(roleName).build();
        return databaseService.getRole(searchObject);
    }

    @Override
    protected Integer getSuccessStatusCode(String input, RoleDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    private String roleNameThatIsNotNullOrBlank(APIGatewayProxyRequestEvent requestInfo) throws BadRequestException {
        return Optional.ofNullable(requestInfo.getPathParameters())
            .map(pathParams -> pathParams.get(ROLE_PATH_PARAMETER))
            .filter(not(String::isBlank))
            .orElseThrow(() -> new BadRequestException(EMPTY_ROLE_NAME));
    }
}
