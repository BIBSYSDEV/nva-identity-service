package no.unit.nva.handlers;

import static java.util.function.Predicate.not;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Optional;
import no.unit.nva.database.DatabaseService;
import no.unit.nva.database.DatabaseServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.BadRequestException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class GetUserHandler extends HandlerAccessingUser<Void, UserDto> {

    private final DatabaseService databaseService;

    @JacocoGenerated
    public GetUserHandler() {
        this(new Environment(), new DatabaseServiceImpl());
    }

    public GetUserHandler(Environment environment, DatabaseService databaseService) {
        super(Void.class, environment);
        this.databaseService = databaseService;
    }

    @Override
    protected UserDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        String username = extractValidUserNameOrThrowException(requestInfo);
        UserDto queryObject = UserDto.newBuilder().withUsername(username).build();
        return databaseService.getUser(queryObject);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, UserDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    private String extractValidUserNameOrThrowException(RequestInfo requestInfo) throws BadRequestException {
        return Optional.of(requestInfo)
                   .map(RequestInfo::getPathParameters)
                   .map(map -> map.get(USERNAME_PATH_PARAMETER))
                   .map(this::decodeUrlPart)
                   .filter(not(String::isBlank))
                   .orElseThrow(() -> new BadRequestException(EMPTY_USERNAME_PATH_PARAMETER_ERROR));
    }
}
