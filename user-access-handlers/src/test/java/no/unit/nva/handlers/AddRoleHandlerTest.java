package no.unit.nva.handlers;

import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.database.DatabaseService;
import no.unit.nva.database.DatabaseServiceImpl;
import no.unit.nva.database.RoleService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessmanagement.exceptions.DataSyncException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RestRequestHandler;
import nva.commons.apigateway.exceptions.InvalidOrMissingTypeException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.zalando.problem.Problem;

public class AddRoleHandlerTest extends HandlerTest {

    public static final String SOME_ROLE_NAME = "someRoleName";
    private RoleDto sampleRole;
    private AddRoleHandler addRoleHandler;
    private Context context;

    /**
     * init.
     *
     * @throws InvalidEntryInternalException when an invalid role is created
     */
    @BeforeEach
    public void init() throws InvalidEntryInternalException {
        context = mock(Context.class);

        DatabaseService service = new DatabaseServiceImpl(initializeTestDatabase(), envWithTableName);
        addRoleHandler = new AddRoleHandler(mockEnvironment(), service);
        sampleRole = sampleRole();
    }

    @Test
    public void handleRequestReturnsBadRequestWhenRequestBodyIsEmpty() throws IOException {
        GatewayResponse<RoleDto> response = sendRequest(null);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    public void handleRequestReturnsBadRequestWhenRequestBodyIsAnEmptyObject() throws IOException {
        GatewayResponse<RoleDto> response = sendRequest(new RoleDto());
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    public void handlerRequestReturnsOkWheRequestBodyIsValid() throws InvalidEntryInternalException, IOException {
        GatewayResponse<RoleDto> response = sendRequest(sampleRole());
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_OK)));
    }

    @Test
    public void handlerRequestReturnsTheGeneratedObjectWhenInputIsValid()
        throws InvalidEntryInternalException, IOException {
        RoleDto actualRole = sampleRole();
        GatewayResponse<RoleDto> response = sendRequest(actualRole);
        RoleDto savedRole = response.getBodyObject(RoleDto.class);
        assertThat(savedRole, is(equalTo(actualRole)));
    }

    @Test
    public void handlerRequestReturnsTheGeneratedObjectAfterWaitingForSyncingToComplete()
        throws InvalidEntryInternalException, IOException {
        RoleDto actualRole = sampleRole();
        DatabaseService service = databaseServiceWithSyncDelay();
        addRoleHandler = new AddRoleHandler(mockEnvironment(), service);

        GatewayResponse<RoleDto> response = sendRequest(actualRole);
        RoleDto savedRole = response.getBodyObject(RoleDto.class);
        assertThat(savedRole, is(equalTo(actualRole)));
    }

    @Test
    public void handleRequestReturnsInternalServerErrorWhenDatabaseFailsToSaveTheData()
        throws InvalidEntryInternalException, IOException {
        RoleDto actualRole = sampleRole();
        DatabaseService service = databaseServiceAddingButNotGettingArole();
        addRoleHandler = new AddRoleHandler(mockEnvironment(), service);

        GatewayResponse<Problem> response = sendRequest(actualRole);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
        Problem problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(AddRoleHandler.ERROR_FETCHING_SAVED_ROLE));
    }

    @Test
    public void statusCodeReturnsOkWhenRequestIsSuccessful() {
        Integer successCode = addRoleHandler.getSuccessStatusCode(null, null);
        assertThat(successCode, is(equalTo(HttpStatus.SC_OK)));
    }

    @Test
    public void addRoleHandlerThrowsDataSyncExceptionWhenDatabaseServiceCannotFetchSavedRole()
        throws InvalidEntryInternalException {

        RoleDto inputRole = sampleRole();
        AddRoleHandler addRoleHandler = addRoleHandlerDoesNotFindRoleAfterAddingIt();
        Executable action = () -> addRoleHandler.processInput(inputRole, null, null);

        DataSyncException exception = assertThrows(DataSyncException.class, action);
        assertThat(exception.getMessage(), containsString(AddRoleHandler.ERROR_FETCHING_SAVED_ROLE));
    }

    @Test
    public void errorMessageIsLoggedWhenAddRoleHandlerThrowsDatasyncException()
        throws IOException {
        TestAppender testingAppender = LogUtils.getTestingAppender(RestRequestHandler.class);

        AddRoleHandler addRoleHandler = addRoleHandlerDoesNotFindRoleAfterAddingIt();
        InputStream inputRequest = new HandlerRequestBuilder<>(objectMapper).withBody(sampleRole).build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        addRoleHandler.handleRequest(inputRequest, outputStream, context);

        assertThat(testingAppender.getMessages(), containsString(AddRoleHandler.ERROR_FETCHING_SAVED_ROLE));
    }

    @Test
    public void handleRequestReturnsBadRequestWhenInputRoleHasNoType()
        throws InvalidEntryInternalException, IOException {
        ObjectNode objectWithoutType = createInputObjectWithoutType(sampleRole());

        GatewayResponse<Problem> response = sendRequestToHandlerWithBody(objectWithoutType);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));

        Problem problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), is(equalTo(InvalidOrMissingTypeException.MESSAGE)));
    }

    private GatewayResponse<Problem> sendRequestToHandlerWithBody(ObjectNode requestBody)
        throws IOException {
        InputStream requestInput = createRequestInputStream(requestBody);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        addRoleHandler.handleRequest(requestInput, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream);
    }

    private RoleDto sampleRole() throws InvalidEntryInternalException {
        return RoleDto.newBuilder().withName(SOME_ROLE_NAME).build();
    }

    private AddRoleHandler addRoleHandlerDoesNotFindRoleAfterAddingIt() {
        DatabaseService databaseNotFoundingRoles = databaseServiceAddingButNotGettingArole();

        return new AddRoleHandler(mockEnvironment(), databaseNotFoundingRoles);
    }

    private DatabaseServiceImpl databaseServiceAddingButNotGettingArole() {
        return new DatabaseServiceImpl(localDynamo, envWithTableName) {

            @Override
            public RoleDto getRole(RoleDto queryObject) throws NotFoundException {
                throw new NotFoundException(RoleService.ROLE_NOT_FOUND_MESSAGE);
            }
        };
    }

    private DatabaseServiceImpl databaseServiceWithSyncDelay() {
        return new DatabaseServiceImpl(localDynamo, envWithTableName) {
            private int counter = 0;

            @Override
            public RoleDto getRole(RoleDto queryObject) throws InvalidEntryInternalException, NotFoundException {
                if (counter == 0) {
                    counter++;
                    throw new NotFoundException(RoleService.ROLE_NOT_FOUND_MESSAGE);
                }
                return super.getRole(queryObject);
            }
        };
    }

    private <T> GatewayResponse<T> sendRequest(RoleDto build) throws IOException {
        InputStream input = new HandlerRequestBuilder<RoleDto>(objectMapper)
            .withBody(build)
            .build();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        addRoleHandler.handleRequest(input, output, context);
        return GatewayResponse.fromOutputStream(output);
    }
}