package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.DataSyncException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class AddUserHandler extends HandlerWithEventualConsistency<UserDto, UserDto> {

    public static final String SYNC_ERROR_MESSAGE = "Error while trying to retrieve saved user:";
    private final IdentityService databaseService;

    /**
     * Default constructor.
     */
    @JacocoGenerated
    public AddUserHandler() {
        this(new Environment(), new IdentityServiceImpl());
    }

    public AddUserHandler(Environment environment,
                          IdentityService databaseService) {
        super(UserDto.class, environment);
        this.databaseService = databaseService;
    }

    @Override
    protected UserDto processInput(UserDto input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        databaseService.addUser(input);
        return getEventuallyConsistent(() -> databaseService.getUser(input))
            .orElseThrow(() -> new DataSyncException(SYNC_ERROR_MESSAGE + input.getUsername()));
    }

    @Override
    protected Integer getSuccessStatusCode(UserDto input, UserDto output) {
        return HttpStatus.SC_OK;
    }
}
