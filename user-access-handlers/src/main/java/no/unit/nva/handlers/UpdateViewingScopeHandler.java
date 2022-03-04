package no.unit.nva.handlers;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.jr.ob.JSON;
import java.net.HttpURLConnection;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import nva.commons.apigatewayv2.exceptions.BadRequestException;

public class UpdateViewingScopeHandler extends HandlerAccessingUser<ViewingScope, Void> {

    private final IdentityService databaseService;

    public UpdateViewingScopeHandler(IdentityService databaseService) {
        super();
        this.databaseService = databaseService;
    }

    @Override
    protected Void processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {
        ViewingScope inputObject = parseInput(input);
        String username = requestInfo.getPathParameters().get(USERNAME_PATH_PARAMETER);
        UserDto queryObject = UserDto.newBuilder().withUsername(username).build();
        UserDto currentUser = databaseService.getUser(queryObject);
        currentUser.setViewingScope(inputObject);
        databaseService.updateUser(currentUser);
        return null;
    }

    private ViewingScope parseInput(String input) {
        return attempt(() -> JSON.std.beanFrom(ViewingScope.class, input))
            .orElseThrow(fail -> new BadRequestException(fail.getException().getMessage()));
    }

    @Override
    protected Integer getSuccessStatusCode(String input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}
