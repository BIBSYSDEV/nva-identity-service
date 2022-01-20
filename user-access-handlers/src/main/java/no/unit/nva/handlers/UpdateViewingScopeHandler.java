package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

public class UpdateViewingScopeHandler extends HandlerAccessingUser<ViewingScope, Void> {

    public static final Environment ENVIRONMENT = new Environment();
    private final IdentityService databaseService;

    public UpdateViewingScopeHandler(IdentityService databaseService) {
        super(ViewingScope.class, ENVIRONMENT);
        this.databaseService = databaseService;
    }

    @Override
    protected Void processInput(ViewingScope input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        String username = requestInfo.getPathParameter(USERNAME_PATH_PARAMETER);
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
