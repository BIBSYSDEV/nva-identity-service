package no.unit.nva.handlers;

import static no.unit.nva.handlers.AddUserHandler.SYNC_ERROR_MESSAGE;
import static no.unit.nva.handlers.EntityUtils.createRequestWithUserWithoutUsername;
import static no.unit.nva.handlers.EntityUtils.createUserWithRolesAndInstitution;
import static no.unit.nva.handlers.EntityUtils.createUserWithoutRoles;
import static no.unit.nva.handlers.EntityUtils.createUserWithoutUsername;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import no.unit.nva.database.DatabaseService;
import no.unit.nva.database.DatabaseServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.DataSyncException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.InvalidOrMissingTypeException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.zalando.problem.Problem;

public class AddUserTest extends HandlerTest {

    public static final String EMPTY_INSTITUTION = null;
    private AddUserHandler handler;
    private RequestInfo requestInfo;
    private Context context;

    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        handler = new AddUserHandler(mockEnvironment(), databaseService);

        requestInfo = new RequestInfo();
        context = mock(Context.class);
    }

    @DisplayName("getSuccessCode returns OK")
    @Test
    public void getSuccessCodeReturnsOk() {
        Integer successCode = handler.getSuccessStatusCode(null, null);
        assertThat(successCode, is(equalTo(HttpStatus.SC_OK)));
    }

    @DisplayName("processInput() adds user to database when input is a user with username and without roles")
    @Test
    public void processInputAddsUserToDatabaseWhenInputIsUserWithUsernameWithoutRoles() throws ApiGatewayException {
        UserDto expectedUser = createUserWithoutRoles();
        UserDto savedUser = handler.processInput(expectedUser, requestInfo, context);
        assertThat(savedUser, is(not(sameInstance(expectedUser))));
        assertThat(savedUser, is(equalTo(expectedUser)));
    }

    @DisplayName("processInput() adds user to database when input is a user with username and roles")
    @Test
    public void processInputAddsUserToDatabaseWhenInputIsUserWithNamesAndRoles() throws ApiGatewayException {
        UserDto expectedUser = createSampleUserWithExistingRoles(DEFAULT_USERNAME, EMPTY_INSTITUTION);
        UserDto savedUser = handler.processInput(expectedUser, requestInfo, context);
        assertThat(savedUser, is(not(sameInstance(expectedUser))));
        assertThat(savedUser, is(equalTo(expectedUser)));
    }

    @DisplayName("processInput() adds user to database when input is a user with username and roles and with"
        + " institutions")
    @Test
    public void processInputAddsUserToDatabaseWhenInputIsUserWithNamesAndRolesAndInstitutions()
        throws ApiGatewayException {
        UserDto expectedUser = createSampleUserWithExistingRoles();

        UserDto savedUser = handler.processInput(expectedUser, requestInfo, context);
        assertThat(savedUser, is(not(sameInstance(expectedUser))));
        assertThat(savedUser, is(equalTo(expectedUser)));
    }

    @DisplayName("processInput() throws ConflictException when input user exists already")
    @Test
    public void processInputThrowsConflictExceptionWhenAddedUserAlreadyExists()
        throws ApiGatewayException {
        UserDto sampleUser = createUserWithRolesAndInstitution();
        addUserFirstTime(sampleUser);
        Executable action = () -> handler.processInput(sampleUser, requestInfo, context);
        assertThrows(ConflictException.class, action);
    }

    @DisplayName("processInput() throws EmptyUsernameException when input user does not have a username")
    @Test
    public void processInputThrowsEmptyUsernameExceptionWhenInputUserDoesNotHaveUsername()
        throws ApiGatewayException, NoSuchMethodException, IllegalAccessException,
               InvocationTargetException {

        UserDto userWithoutUsername = createUserWithoutUsername();

        Executable action = () -> handler.processInput(userWithoutUsername, requestInfo, context);
        assertThrows(InvalidInputException.class, action);
    }

    @DisplayName("handleRequest() returns BadRequest when input user does not have a username")
    @Test
    public void processInputThrowsConflictExceptionWhenInputUserDoesNotHaveUsername()
        throws ApiGatewayException, IOException, NoSuchMethodException, IllegalAccessException,
               InvocationTargetException {

        InputStream requestWithUserWithoutUsername = createRequestWithUserWithoutUsername();
        ByteArrayOutputStream outputStream = sendRequestToHandler(requestWithUserWithoutUsername);

        GatewayResponse<Problem> response = parseResponseStream(outputStream);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @DisplayName("processInput() throws DataSyncException when database service cannot return saved item ")
    @Test
    public void processInputThrowsDataSyncExceptionWhenDatabaseServiceCannotReturnSavedItem()
        throws ApiGatewayException {
        DatabaseService databaseService = databaseServiceReturnsAlwaysEmptyUser();

        UserDto sampleUser = createUserWithRolesAndInstitution();
        AddUserHandler addUserHandler = new AddUserHandler(mockEnvironment(), databaseService);
        Executable action = () -> addUserHandler.processInput(sampleUser, requestInfo, context);
        DataSyncException exception = assertThrows(DataSyncException.class, action);
        assertThat(exception.getMessage(), containsString(SYNC_ERROR_MESSAGE));
    }

    @DisplayName("handleRequest() returns BadRequest when input object has no type")
    @Test
    public void handlerRequestReturnsBadRequestWhenInputObjectHasNoType()
        throws InvalidEntryInternalException, IOException {

        UserDto sampleUser = createUserWithRolesAndInstitution();
        ObjectNode inputObjectWithoutType = createInputObjectWithoutType(sampleUser);
        InputStream requestInputStream = createRequestInputStream(inputObjectWithoutType);

        ByteArrayOutputStream outputStream = sendRequestToHandler(requestInputStream);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));

        Problem problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), is(equalTo(InvalidOrMissingTypeException.MESSAGE)));
    }

    private ByteArrayOutputStream sendRequestToHandler(InputStream requestInputStream)
        throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        handler.handleRequest(requestInputStream, outputStream, context);
        return outputStream;
    }

    private DatabaseService databaseServiceReturnsAlwaysEmptyUser() {
        return new DatabaseServiceImpl(localDynamo, envWithTableName) {
            @Override
            public UserDto getUser(UserDto queryObject) {
                return null;
            }
        };
    }

    private GatewayResponse<Problem> parseResponseStream(ByteArrayOutputStream outputStream)
        throws IOException {
        String outputString = outputStream.toString();
        TypeReference<GatewayResponse<Problem>> typeReference = new TypeReference<>() {};
        return objectMapper.readValue(outputString, typeReference);
    }

    private void addUserFirstTime(UserDto inputUser) throws ApiGatewayException {
        handler.processInput(inputUser, requestInfo, context);
    }
}
