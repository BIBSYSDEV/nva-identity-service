package no.unit.nva.handlers;

import static java.util.function.Predicate.not;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.Optional;
import no.unit.nva.database.DatabaseService;
import no.unit.nva.database.DatabaseServiceImpl;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.UserList;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class ListByInstitutionHandler extends ApiGatewayHandler<Void, UserList> {

    public static final String INSTITUTION_ID_QUERY_PARAMETER = "institution";
    public static final String MISSING_QUERY_PARAMETER_ERROR = "Missing institution path parameter. "
        + "Probably error in the Lambda function definition.";
    private final DatabaseService databaseService;

    @SuppressWarnings("unused")
    @JacocoGenerated
    public ListByInstitutionHandler() {
        this(new Environment(), new DatabaseServiceImpl());
    }

    public ListByInstitutionHandler(Environment environment, DatabaseService databaseService) {
        super(Void.class, environment);
        this.databaseService = databaseService;
    }

    @Override
    protected UserList processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        String institutionId = extractInstitutionIdFromRequest(requestInfo);
        List<UserDto> users = databaseService.listUsers(institutionId);
        return UserList.fromList(users);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, UserList output) {
        return HttpStatus.SC_OK;
    }

    private String extractInstitutionIdFromRequest(RequestInfo requestInfo) {
        return Optional.of(requestInfo)
            .map(RequestInfo::getQueryParameters)
            .map(queryParams -> queryParams.get(INSTITUTION_ID_QUERY_PARAMETER))
            .filter(not(String::isBlank))
            .orElseThrow(() -> new IllegalStateException(MISSING_QUERY_PARAMETER_ERROR));
    }
}
