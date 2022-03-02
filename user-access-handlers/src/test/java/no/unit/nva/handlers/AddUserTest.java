package no.unit.nva.handlers;

import static no.unit.nva.handlers.AddUserHandler.SYNC_ERROR_MESSAGE;
import static no.unit.nva.handlers.EntityUtils.createRequestWithUserWithoutUsername;
import static no.unit.nva.handlers.EntityUtils.createUserWithRolesAndInstitution;
import static no.unit.nva.handlers.EntityUtils.createUserWithoutRoles;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AddUserTest extends HandlerTest {

    public static final URI EMPTY_INSTITUTION = null;
    private AddUserHandler handler;
    private Context context;

    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        handler = new AddUserHandler(databaseService);

        context = mock(Context.class);
    }



    @DisplayName("processInput() adds user to database when input is a user with username and without roles")
    @Test
    void processInputAddsUserToDatabaseWhenInputIsUserWithUsernameWithoutRoles() {
        var expectedUser = createUserWithoutRoles();
        var savedUser = sendRequest(expectedUser);
        assertThat(savedUser, is(not(sameInstance(expectedUser))));
        assertThat(savedUser, is(equalTo(expectedUser)));
    }

    @DisplayName("processInput() adds user to database when input is a user with username and roles")
    @Test
    void processInputAddsUserToDatabaseWhenInputIsUserWithNamesAndRoles() {
        UserDto expectedUser = createSampleUserWithExistingRoles(DEFAULT_USERNAME, EMPTY_INSTITUTION);
        UserDto savedUser = sendRequest(expectedUser);
        assertThat(savedUser, is(not(sameInstance(expectedUser))));
        assertThat(savedUser, is(equalTo(expectedUser)));
    }

    @DisplayName("processInput() adds user to database when input is a user with username and roles and with"
                 + " institutions")
    @Test
    void processInputAddsUserToDatabaseWhenInputIsUserWithNamesAndRolesAndInstitutions() {
        var expectedUser = createSampleUserWithExistingRoles();
        var savedUser = sendRequest(expectedUser);
        assertThat(savedUser, is(not(sameInstance(expectedUser))));
        assertThat(savedUser, is(equalTo(expectedUser)));
    }

    @DisplayName("processInput() throws ConflictException when input user exists already")
    @Test
    void processInputThrowsConflictExceptionWhenAddedUserAlreadyExists() {
        var sampleUser = createUserWithRolesAndInstitution();
        addUserFirstTime(sampleUser);
        var response = handler.handleRequest(createRequest(sampleUser), context);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
    }

    @Test
    void handleRequestReturnsBadRequestWhenInputUserDoesNotHaveUsername() throws IOException {
        var requestWithUserWithoutUsername = createRequestWithUserWithoutUsername();
        var response= sendRequestToHandler(requestWithUserWithoutUsername);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @DisplayName("processInput() throws DataSyncException when database service cannot return saved item ")
    @Test
    void processInputThrowsDataSyncExceptionWhenDatabaseServiceCannotReturnSavedItem() {
        IdentityService databaseService = databaseServiceReturnsAlwaysEmptyUser();

        UserDto sampleUser = createUserWithRolesAndInstitution();
        AddUserHandler addUserHandler = new AddUserHandler( databaseService);
        var response = addUserHandler.handleRequest(createRequest(sampleUser),context);
        assertThat(response.getBody(), containsString(SYNC_ERROR_MESSAGE));
    }

    @DisplayName("handleRequest() returns BadRequest when input object has no type")
    @Test
    @Disabled("We cannot enforice the existence of type with Jackson Jr")
    void handlerRequestReturnsBadRequestWhenInputObjectHasNoType()
        throws InvalidEntryInternalException {

    }

    private UserDto sendRequest(UserDto expectedUser) {
        APIGatewayProxyRequestEvent request = createRequest(
            expectedUser);
        var response = handler.handleRequest(request, context);
        return attempt(() -> JSON.std.beanFrom(UserDto.class, response.getBody())).orElseThrow();
    }

    private APIGatewayProxyRequestEvent createRequest(UserDto expectedUser) {
        var bodyString = attempt(() -> JSON.std.asString(expectedUser)).orElseThrow();
        return new APIGatewayProxyRequestEvent().withBody(bodyString);
    }

    private APIGatewayProxyResponseEvent sendRequestToHandler(APIGatewayProxyRequestEvent requestInputStream) {
        return handler.handleRequest(requestInputStream, context);
    }

    private IdentityService databaseServiceReturnsAlwaysEmptyUser() {
        return new IdentityServiceImpl(localDynamo) {
            @Override
            public UserDto getUser(UserDto queryObject) {
                return null;
            }
        };
    }

    private void addUserFirstTime(UserDto inputUser) {
        sendRequest(inputUser);
    }
}
