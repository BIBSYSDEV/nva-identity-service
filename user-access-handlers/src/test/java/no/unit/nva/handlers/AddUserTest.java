package no.unit.nva.handlers;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.handlers.AddUserHandler.SYNC_ERROR_MESSAGE;
import static no.unit.nva.handlers.EntityUtils.createRequestWithUserWithoutUsername;
import static no.unit.nva.handlers.EntityUtils.createUserWithRolesAndInstitution;
import static no.unit.nva.handlers.EntityUtils.createUserWithoutRoles;
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
import java.net.URI;
import no.unit.nva.Constants;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.DataSyncException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.zalando.problem.Problem;

class AddUserTest extends HandlerTest {

    public static final URI EMPTY_INSTITUTION = null;
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
    void getSuccessCodeReturnsOk() {
        Integer successCode = handler.getSuccessStatusCode(null, null);
        assertThat(successCode, is(equalTo(HttpStatus.SC_OK)));
    }

    @DisplayName("processInput() adds user to database when input is a user with username and without roles")
    @Test
    void processInputAddsUserToDatabaseWhenInputIsUserWithUsernameWithoutRoles() throws ApiGatewayException {
        UserDto expectedUser = createUserWithoutRoles();
        UserDto savedUser = handler.processInput(expectedUser, requestInfo, context);
        assertThat(savedUser, is(not(sameInstance(expectedUser))));
        assertThat(savedUser, is(equalTo(expectedUser)));
    }

    @DisplayName("processInput() adds user to database when input is a user with username and roles")
    @Test
    void processInputAddsUserToDatabaseWhenInputIsUserWithNamesAndRoles() throws ApiGatewayException {
        UserDto expectedUser = createSampleUserWithExistingRoles(DEFAULT_USERNAME, EMPTY_INSTITUTION);
        UserDto savedUser = handler.processInput(expectedUser, requestInfo, context);
        assertThat(savedUser, is(not(sameInstance(expectedUser))));
        assertThat(savedUser, is(equalTo(expectedUser)));
    }

    @DisplayName("processInput() adds user to database when input is a user with username and roles and with"
        + " institutions")
    @Test
    void processInputAddsUserToDatabaseWhenInputIsUserWithNamesAndRolesAndInstitutions()
        throws ApiGatewayException {
        UserDto expectedUser = createSampleUserWithExistingRoles();

        UserDto savedUser = handler.processInput(expectedUser, requestInfo, context);
        assertThat(savedUser, is(not(sameInstance(expectedUser))));
        assertThat(savedUser, is(equalTo(expectedUser)));
    }

    @DisplayName("processInput() throws ConflictException when input user exists already")
    @Test
    void processInputThrowsConflictExceptionWhenAddedUserAlreadyExists()
        throws ApiGatewayException {
        UserDto sampleUser = createUserWithRolesAndInstitution();
        addUserFirstTime(sampleUser);
        Executable action = () -> handler.processInput(sampleUser, requestInfo, context);
        assertThrows(ConflictException.class, action);
    }

    @Test
    void handleRequestReturnsBadRequestWhenInputUserDoesNotHaveUsername() throws  IOException {

        InputStream requestWithUserWithoutUsername = createRequestWithUserWithoutUsername();
        ByteArrayOutputStream outputStream = sendRequestToHandler(requestWithUserWithoutUsername);

        GatewayResponse<Problem> response = parseResponseStream(outputStream);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @DisplayName("processInput() throws DataSyncException when database service cannot return saved item ")
    @Test
    void processInputThrowsDataSyncExceptionWhenDatabaseServiceCannotReturnSavedItem() {
        IdentityService databaseService = databaseServiceReturnsAlwaysEmptyUser();

        UserDto sampleUser = createUserWithRolesAndInstitution();
        AddUserHandler addUserHandler = new AddUserHandler(mockEnvironment(), databaseService);
        Executable action = () -> addUserHandler.processInput(sampleUser, requestInfo, context);
        DataSyncException exception = assertThrows(DataSyncException.class, action);
        assertThat(exception.getMessage(), containsString(SYNC_ERROR_MESSAGE));
    }

    @DisplayName("handleRequest() returns BadRequest when input object has no type")
    @Test
    void handlerRequestReturnsBadRequestWhenInputObjectHasNoType()
        throws InvalidEntryInternalException, IOException {

        UserDto sampleUser = createUserWithRolesAndInstitution();
        ObjectNode inputObjectWithoutType = createInputObjectWithoutType(sampleUser);
        InputStream requestInputStream = createRequestInputStream(inputObjectWithoutType);

        ByteArrayOutputStream outputStream = sendRequestToHandler(requestInputStream);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));

        Problem problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(Constants.COULD_NOT_RESOLVE_SUBTYPE_OF));
    }

    private ByteArrayOutputStream sendRequestToHandler(InputStream requestInputStream)
        throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        handler.handleRequest(requestInputStream, outputStream, context);
        return outputStream;
    }


    private IdentityService databaseServiceReturnsAlwaysEmptyUser() {
        return new IdentityServiceImpl(localDynamo) {
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
        return dtoObjectMapper.readValue(outputString, typeReference);
    }

    private void addUserFirstTime(UserDto inputUser) throws ApiGatewayException {
        handler.processInput(inputUser, requestInfo, context);
    }
}
