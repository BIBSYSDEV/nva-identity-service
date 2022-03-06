package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;

public class UpdateViewingScopeHandler extends HandlerAccessingUser<ViewingScope, Void> {

    private final IdentityService databaseService;

    public UpdateViewingScopeHandler(IdentityService databaseService) {
        super();
        this.databaseService = databaseService;
    }

    @Override
    protected Void processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {
        ViewingScope inputObject = ViewingScope.fromJson(input);
        String username = requestInfo.getPathParameters().get(USERNAME_PATH_PARAMETER);
        UserDto queryObject = UserDto.newBuilder().withUsername(username).build();
        UserDto currentUser = databaseService.getUser(queryObject);
        currentUser.setViewingScope(inputObject);
        databaseService.updateUser(currentUser);
        return null;
    }



    @Override
    protected Integer getSuccessStatusCode(String input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}
