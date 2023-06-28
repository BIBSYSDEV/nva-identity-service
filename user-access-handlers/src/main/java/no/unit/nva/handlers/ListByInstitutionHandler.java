package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.UserList;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class ListByInstitutionHandler extends ApiGatewayHandler<Void, UserList> {

    public static final String INSTITUTION_ID_QUERY_PARAMETER = "institution";
    public static final String MISSING_QUERY_PARAMETER_ERROR = "Institution Id query parameter is not a URI. "
                                                               + "Probably error in the Lambda function definition.";
    private final IdentityService databaseService;

    private final BiPredicate<UserDto, Optional<String>> likeUserOrEmpty = (userDto, userName) -> userName
               .map(s -> userDto.getUsername().contains(s))
               .orElse(true);
    private final BiPredicate<UserDto, Optional<String>> hasRoleOrEmpty = (userDto, roleName) ->
              roleName
                  .map(s -> userDto.getRoles().stream().anyMatch(f -> f.getRoleName().equals(s)))
                  .orElse(true);

    public ListByInstitutionHandler(IdentityService databaseService) {
        super(Void.class);
        this.databaseService = databaseService;
    }

    @SuppressWarnings("unused")
    @JacocoGenerated
    public ListByInstitutionHandler() {
        this(new IdentityServiceImpl());
    }

    @Override
    protected Integer getSuccessStatusCode(Void body, UserList output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected UserList processInput(Void body, RequestInfo input, Context context) throws BadRequestException {
        var institutionId = extractInstitutionIdFromRequest(input);
        var role = input.getQueryParameterOpt("role");
        var userName = input.getQueryParameterOpt("name");
        var users =
            databaseService
                .listUsers(institutionId).stream()
                .filter(user -> likeUserOrEmpty.test(user, userName))
                .filter(user -> hasRoleOrEmpty.test(user, role))
                .collect(Collectors.toList());
        return UserList.fromList(users);
    }

    private URI extractInstitutionIdFromRequest(RequestInfo requestInfo) throws BadRequestException {
        return requestInfo.getQueryParameterOpt(INSTITUTION_ID_QUERY_PARAMETER)
                   .map(URI::create)
                   .orElseThrow(() -> new BadRequestException(MISSING_QUERY_PARAMETER_ERROR));
    }
}
