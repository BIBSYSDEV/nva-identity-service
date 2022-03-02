package no.unit.nva.handlers;

import static java.util.function.Predicate.not;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.UserList;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.apigatewayv2.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class ListByInstitutionHandler extends ApiGatewayHandlerV2<Void, UserList> {

    public static final String INSTITUTION_ID_QUERY_PARAMETER = "institution";
    public static final String MISSING_QUERY_PARAMETER_ERROR = "Institution Id query parameter is not a URI. "
                                                               + "Probably error in the Lambda function definition.";
    private final IdentityService databaseService;

    public ListByInstitutionHandler(IdentityService databaseService) {
        this.databaseService = databaseService;
    }

    @SuppressWarnings("unused")
    @JacocoGenerated
    public ListByInstitutionHandler() {
        this(new IdentityServiceImpl());
    }

    @Override
    protected Integer getSuccessStatusCode(String body, UserList output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected UserList processInput(String body, APIGatewayProxyRequestEvent input, Context context)
        throws ApiGatewayException {
        URI institutionId = extractInstitutionIdFromRequest(input);
        List<UserDto> users = databaseService.listUsers(institutionId);
        return UserList.fromList(users);
    }

    private URI extractInstitutionIdFromRequest(APIGatewayProxyRequestEvent requestInfo) {
        return Optional.of(requestInfo)
            .map(APIGatewayProxyRequestEvent::getQueryStringParameters)
            .map(queryParams -> queryParams.getOrDefault(INSTITUTION_ID_QUERY_PARAMETER, ""))
            .filter(not(String::isBlank))
            .map(URI::create)
            .orElseThrow(() -> new IllegalStateException(MISSING_QUERY_PARAMETER_ERROR));
    }
}
