package no.unit.nva.handlers;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.jr.ob.JSON;
import java.net.HttpURLConnection;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.DataSyncException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class AddUserHandler extends HandlerWithEventualConsistency<UserDto, UserDto> {

    public static final String SYNC_ERROR_MESSAGE = "Error while trying to retrieve saved user:";
    private final IdentityService databaseService;

    /**
     * Default constructor.
     */
    @JacocoGenerated
    public AddUserHandler() {
        this(new IdentityServiceImpl());
    }

    public AddUserHandler(IdentityService databaseService) {
        super();
        this.databaseService = databaseService;
    }

    @Override
    protected UserDto processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {
        var inputObject = parseUser(input);
        databaseService.addUser(inputObject);
        return getEventuallyConsistent(() -> databaseService.getUser(inputObject))
            .orElseThrow(() -> new DataSyncException(SYNC_ERROR_MESSAGE + inputObject.getUsername()));
    }

    private UserDto parseUser(String input) {
        return attempt(() -> JSON.std.beanFrom(UserDto.class, input))
            .orElseThrow(fail -> new BadRequestException(fail.getException().getMessage()));
    }

    @Override
    protected Integer getSuccessStatusCode(String input, UserDto output) {
        return HttpURLConnection.HTTP_OK;
    }
}
