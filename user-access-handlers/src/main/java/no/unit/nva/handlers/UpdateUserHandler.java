package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
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
        super(UserDto.class);
        this.databaseService = databaseService;
    }

    @Override
    protected Void processInput(UserDto input, RequestInfo requestInfo, Context context)
        throws NotFoundException, InvalidInputException {

        validateRequest(input, requestInfo);
        databaseService.updateUser(input);
        addAdditionalHeaders(addLocationHeaderToResponseSupplier(input));
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(UserDto input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
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
