package no.unit.nva.handlers;

import static java.util.function.Predicate.not;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.database.DatabaseService;
import no.unit.nva.database.DatabaseServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.BadRequestException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class GetRoleHandler extends ApiGatewayHandler<Void, RoleDto> {

    public static final String EMPTY_ROLE_NAME = "Role-name cannot be empty";
    public static final String ROLE_PATH_PARAMETER = "role";

    private final DatabaseService databaseService;

    /**
     * Default constructor used by AWS Lambda.
     */
    @JacocoGenerated
    public GetRoleHandler() {
        this(new Environment(), new DatabaseServiceImpl());
    }

    public GetRoleHandler(Environment environment, DatabaseService databaseService) {
        super(Void.class, environment);
        this.databaseService = databaseService;
    }

    @Override
    public RoleDto processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        String roleName = roleNameThatIsNotNullOrBlank(requestInfo);

        RoleDto searchObject = RoleDto.newBuilder().withName(roleName).build();
        return databaseService.getRole(searchObject);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, RoleDto output) {
        return HttpStatus.SC_OK;
    }

    private String roleNameThatIsNotNullOrBlank(RequestInfo requestInfo) throws BadRequestException {
        return Optional.ofNullable(requestInfo.getPathParameters())
            .map(pathParams -> pathParams.get(ROLE_PATH_PARAMETER))
            .filter(not(String::isBlank))
            .orElseThrow(() -> new BadRequestException(EMPTY_ROLE_NAME));
    }
}
