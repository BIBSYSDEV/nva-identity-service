package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.DatabaseService;
import no.unit.nva.database.DatabaseServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.DataSyncException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddUserHandler extends HandlerWithEventualConsistency<UserDto, UserDto> {

    public static final String SYNC_ERROR_MESSAGE = "Error while trying to retrieve saved user:";
    public static final String PUBLIC_MESSAGE_FOR_INTERNAL_CONSISTENCY_PROBLEMS = "Α problem with the data has occured";
    public static final String INCONSISTENT_DATA_ERROR = "Inconsistent data in the database.";
    private static final Logger logger = LoggerFactory.getLogger(AddUserHandler.class);
    private final DatabaseService databaseService;

    /**
     * Default constructor.
     */
    @JacocoGenerated
    public AddUserHandler() {
        this(new Environment(), new DatabaseServiceImpl());
    }

    public AddUserHandler(Environment environment,
                          DatabaseService databaseService) {
        super(UserDto.class, environment);
        this.databaseService = databaseService;
    }

    @Override
    protected UserDto processInput(UserDto input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        tryAddingUser(input);

        return getEventuallyConsistent(() -> getUser(input))
                   .orElseThrow(() -> new DataSyncException(SYNC_ERROR_MESSAGE + input.getUsername()));
    }

    @Override
    protected Integer getSuccessStatusCode(UserDto input, UserDto output) {
        return HttpStatus.SC_OK;
    }

    private void tryAddingUser(UserDto input)
        throws ConflictException, InvalidInputException {
        try {
            databaseService.addUser(input);
        } catch (InvalidEntryInternalException e) {
            logger.error(INCONSISTENT_DATA_ERROR, e);
            throw new RuntimeException(PUBLIC_MESSAGE_FOR_INTERNAL_CONSISTENCY_PROBLEMS);
        }
    }

    private UserDto getUser(UserDto input) throws NotFoundException, InvalidEntryInternalException {
        return databaseService.getUser(input);
    }
}
