package no.unit.nva.handlers;

import static java.util.function.Predicate.not;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.util.Optional;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class GetUserHandler extends HandlerAccessingUser<Void, UserDto> {

    private final IdentityService databaseService;

    @JacocoGenerated
    public GetUserHandler() {
        this(new IdentityServiceImpl());
    }

    public GetUserHandler(IdentityService databaseService) {
        super();
        this.databaseService = databaseService;

    }

    @Override
    protected UserDto processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {

        String username = extractValidUserNameOrThrowException(requestInfo);
        UserDto queryObject = UserDto.newBuilder().withUsername(username).build();
        return databaseService.getUser(queryObject);
    }

    @Override
    protected Integer getSuccessStatusCode(String input, UserDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    private String extractValidUserNameOrThrowException(APIGatewayProxyRequestEvent requestInfo) {
        return Optional.of(requestInfo)
            .map(APIGatewayProxyRequestEvent::getPathParameters)
            .map(map -> map.get(USERNAME_PATH_PARAMETER))
            .map(this::decodeUrlPart)
            .filter(not(String::isBlank))
            .orElseThrow(() -> new BadRequestException(EMPTY_USERNAME_PATH_PARAMETER_ERROR));
    }
}
