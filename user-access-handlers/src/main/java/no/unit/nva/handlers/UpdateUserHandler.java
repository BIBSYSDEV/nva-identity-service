package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_AFFILIATION;

public class UpdateUserHandler extends HandlerAccessingUser<UserDto, Void> {

    public static final String LOCATION_HEADER = "Location";

    public static final String INCONSISTENT_USERNAME_IN_PATH_AND_OBJECT_ERROR =
            "Path username is different from input object's user-id";
    private final IdentityService databaseService;

    @JacocoGenerated
    public UpdateUserHandler() {
        this(new IdentityServiceImpl());
    }

    public UpdateUserHandler(IdentityService databaseService) {
        super(UserDto.class);
        this.databaseService = databaseService;
    }

    @Override
    protected void validateRequest(UserDto userDto, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        authorizeRequest(userDto, requestInfo);
        validateRequest(userDto, requestInfo);
    }

    @Override
    protected Void processInput(UserDto input, RequestInfo requestInfo, Context context)
            throws NotFoundException, InvalidInputException, ForbiddenException {

        databaseService.updateUser(input);
        addAdditionalHeaders(addLocationHeaderToResponseSupplier(input));
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(UserDto input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }

    private void authorizeRequest(UserDto input, RequestInfo requestInfo) throws ForbiddenException, NotFoundException {
        if (!isAuthorized(input, requestInfo)) {
            throw new ForbiddenException();
        }
    }

    private boolean isAuthorized(UserDto input, RequestInfo requestInfo) throws NotFoundException {
        return requestInfo.clientIsInternalBackend()
                || requestInfo.userIsAuthorized(MANAGE_CUSTOMERS)
                || isAuthorizedUser(input, requestInfo);
    }

    private boolean isAuthorizedUser(UserDto input, RequestInfo requestInfo) throws NotFoundException {
        var isAlteringAppAdmin = isAlteringAppAdmin(input);

        if (isAlteringAppAdmin) {
            return false;
        }

        return requestInfo.userIsAuthorized(MANAGE_OWN_AFFILIATION);
    }

    private boolean isAlteringAppAdmin(UserDto input) throws NotFoundException {
        var newRoles = input.getRoles().stream().map(RoleDto::getRoleName).collect(Collectors.toSet());
        var existingRoles =
                databaseService.getUser(input).getRoles().stream().map(RoleDto::getRoleName).collect(Collectors.toSet());
        return newRoles.contains(RoleName.APPLICATION_ADMIN) != existingRoles.contains(RoleName.APPLICATION_ADMIN);
    }

    private void validateRequest(UserDto input, RequestInfo requestInfo) throws InvalidInputException {
        String userIdFromPath = extractUsernameFromPathParameters(requestInfo);
        comparePathAndInputObjectUsername(input, userIdFromPath);
    }

    private String extractUsernameFromPathParameters(RequestInfo requestInfo) {
        return Optional.ofNullable(requestInfo.getPathParameters())
                .flatMap(pathParams -> Optional.ofNullable(pathParams.get(USERNAME_PATH_PARAMETER)))
                .map(this::decodeUrlPart)
                .orElseThrow(() -> new RuntimeException(EMPTY_USERNAME_PATH_PARAMETER_ERROR));
    }

    private void comparePathAndInputObjectUsername(UserDto input, String userIdFromPathParameter)
            throws InvalidInputException {
        if (!userIdFromPathParameter.equals(input.getUsername())) {
            throw new InvalidInputException(INCONSISTENT_USERNAME_IN_PATH_AND_OBJECT_ERROR);
        }
    }

    private Supplier<Map<String, String>> addLocationHeaderToResponseSupplier(UserDto input) {
        String location = createUserLocationPath(input);
        return () -> Collections.singletonMap(LOCATION_HEADER, location);
    }

    private String createUserLocationPath(UserDto input) {
        return USERS_RELATIVE_PATH + input.getUsername();
    }
}
