package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.UserList;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ListByInstitutionHandler extends ApiGatewayHandler<Void, UserList> {

    public static final String INSTITUTION_ID_QUERY_PARAMETER = "institution";
    public static final String MISSING_QUERY_PARAMETER_ERROR = "Institution Id query parameter is not a URI. "
        + "Probably error in the Lambda function definition.";
    public static final String QUERY_PARAM_ROLE = "role";
    public static final String QUERY_PARAM_NAME = "name";
    private final IdentityService databaseService;

    @SuppressWarnings("unused")
    @JacocoGenerated
    public ListByInstitutionHandler() {
        this(new IdentityServiceImpl());
    }

    public ListByInstitutionHandler(IdentityService databaseService) {
        super(Void.class);
        this.databaseService = databaseService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected UserList processInput(Void body, RequestInfo input, Context context) throws BadRequestException {
        var institutionId = extractInstitutionIdFromRequest(input);
        var roles = new HashSet<>(input.getMultiValueQueryParameter(QUERY_PARAM_ROLE));
        var userName = input.getQueryParameterOpt(QUERY_PARAM_NAME);
        var users =
            databaseService
                .listUsers(institutionId).stream()
                .filter(user -> likeUserOrEmpty(user, userName))
                .filter(user -> roles.isEmpty() || hasOneRole(user, roles))
                .collect(Collectors.toList());
        return UserList.fromList(users);
    }

    @Override
    protected Integer getSuccessStatusCode(Void body, UserList output) {
        return HttpURLConnection.HTTP_OK;
    }

    private URI extractInstitutionIdFromRequest(RequestInfo requestInfo) throws BadRequestException {
        return requestInfo.getQueryParameterOpt(INSTITUTION_ID_QUERY_PARAMETER)
            .map(URI::create)
            .orElseThrow(() -> new BadRequestException(MISSING_QUERY_PARAMETER_ERROR));
    }

    private boolean likeUserOrEmpty(UserDto userDto, Optional<String> userName) {
        return userName
            .map(s -> userDto.getUsername().contains(s))
            .orElse(true);
    }

    private boolean hasOneRole(UserDto userDto, Set<String> checkedRoles) {
        var userRoles =
            userDto.getRoles().stream().map(RoleDto::getRoleName).map(RoleName::getValue).collect(Collectors.toSet());
        return !Collections.disjoint(userRoles, checkedRoles);
    }
}
