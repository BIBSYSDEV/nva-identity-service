package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.ViewingScope;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;

import java.net.HttpURLConnection;

public class UpdateViewingScopeHandler extends HandlerAccessingUser<ViewingScope, Void> {

    private final IdentityService databaseService;

    public UpdateViewingScopeHandler(IdentityService databaseService) {
        super(ViewingScope.class);
        this.databaseService = databaseService;
    }

    @Override
    protected void validateRequest(ViewingScope viewingScope, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Void processInput(ViewingScope input, RequestInfo requestInfo, Context context) throws NotFoundException {

        String username = requestInfo.getPathParameters().get(USERNAME_PATH_PARAMETER);
        UserDto queryObject = UserDto.newBuilder().withUsername(username).build();
        UserDto currentUser = databaseService.getUser(queryObject);
        currentUser.setViewingScope(input);
        databaseService.updateUser(currentUser);
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(ViewingScope input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}
