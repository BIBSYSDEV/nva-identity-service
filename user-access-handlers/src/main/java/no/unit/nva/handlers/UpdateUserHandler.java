package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.JacocoGenerated;

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
        super();
        this.databaseService = databaseService;
    }

    @Override
    protected Integer getSuccessStatusCode(String input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }

    @Override
    protected Void processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {
        UserDto inputObject = UserDto.fromJson(input);
        validateRequest(inputObject, requestInfo);
        databaseService.updateUser(inputObject);
        addAdditionalSuccessHeaders(addLocationHeaderToResponseSupplier(inputObject));
        return null;
    }



    private void validateRequest(UserDto input, APIGatewayProxyRequestEvent requestInfo) {
        String userIdFromPath = extractUsernameFromPathParameters(requestInfo);
        comparePathAndInputObjectUsername(input, userIdFromPath);
    }

    private String extractUsernameFromPathParameters(APIGatewayProxyRequestEvent requestInfo) {
        return Optional.ofNullable(requestInfo.getPathParameters())
            .flatMap(pathParams -> Optional.ofNullable(pathParams.get(USERNAME_PATH_PARAMETER)))
            .map(this::decodeUrlPart)
            .orElseThrow(() -> new RuntimeException(EMPTY_USERNAME_PATH_PARAMETER_ERROR));
    }

    private void comparePathAndInputObjectUsername(UserDto input, String userIdFromPathParameter) {
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
