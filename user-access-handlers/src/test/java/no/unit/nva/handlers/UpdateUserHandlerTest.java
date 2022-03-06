package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.database.UserService.USER_NOT_FOUND_MESSAGE;
import static no.unit.nva.handlers.EntityUtils.createUserWithoutUsername;
import static no.unit.nva.handlers.UpdateUserHandler.INCONSISTENT_USERNAME_IN_PATH_AND_OBJECT_ERROR;
import static no.unit.nva.handlers.UpdateUserHandler.LOCATION_HEADER;
import static no.unit.nva.handlers.UpdateUserHandler.USERNAME_PATH_PARAMETER;
import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static no.unit.nva.useraccessmanagement.model.UserDto.VIEWING_SCOPE_FIELD;
import static no.unit.nva.useraccessmanagement.model.ViewingScope.INCLUDED_UNITS;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.apigatewayv2.exceptions.ApiGatewayException;
import nva.commons.apigatewayv2.exceptions.ConflictException;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class UpdateUserHandlerTest extends HandlerTest {

    public static final String SAMPLE_ROLE = "someRole";
    public static final String SAMPLE_USERNAME = "some@somewhere";
    public static final URI SAMPLE_INSTITUTION = randomCristinOrgId();

    public static final String ANOTHER_ROLE = "ANOTHER_ROLE";
    public static final String SOME_OTHER_USERNAME = "SomeOtherUsername";
    private IdentityServiceImpl databaseService;
    private Context context;

    @BeforeEach
    public void init() {
        databaseService = new IdentityServiceImpl(initializeTestDatabase());
        context = new FakeContext();
    }

    @DisplayName("handleRequest() returns Location header with the URI to updated user when path contains "
                 + "an existing user id, and the body contains a valid UserDto and path id is the same as the body id ")
    @Test
    void handleRequestReturnsUpdatedUserWhenPathAndBodyContainTheSameUserIdAndTheIdExistsAndBodyIsValid()
        throws ApiGatewayException, IOException {

        UserDto userUpdate = createUserUpdateOnExistingUser();

        var gatewayResponse = sendUpdateRequest(userUpdate.getUsername(), userUpdate);
        Map<String, String> responseHeaders = gatewayResponse.getHeaders();

        assertThat(responseHeaders, hasKey(LOCATION_HEADER));

        String expectedLocationPath = UpdateUserHandler.USERS_RELATIVE_PATH + userUpdate.getUsername();
        assertThat(responseHeaders.get(LOCATION_HEADER), is(equalTo(expectedLocationPath)));
    }

    @DisplayName("handleRequest() can process URL encoded usernames")
    @Test
    void handleRequestCanProcessUrlEncodedUsernames()
        throws ApiGatewayException, IOException {

        UserDto userUpdate = createUserUpdateOnExistingUser();

        String encodedUsername = encodeString(userUpdate.getUsername());
        var gatewayResponse = sendUpdateRequest(encodedUsername, userUpdate);
        Map<String, String> responseHeaders = gatewayResponse.getHeaders();

        assertThat(responseHeaders, hasKey(LOCATION_HEADER));

        String expectedLocationPath = UpdateUserHandler.USERS_RELATIVE_PATH + userUpdate.getUsername();
        assertThat(responseHeaders.get(LOCATION_HEADER), is(equalTo(expectedLocationPath)));
    }

    @DisplayName("handleRequest() returns 202 (Accepted)  when path contains "
                 + "an existing user id, and the body contains a valid UserDto and path id is the same as the body id ")
    @Test
    public void processInputReturnsAcceptedWhenPathAndBodyContainTheSameUserIdAndTheIdExistsAndBodyIsValid()
        throws ApiGatewayException, IOException {

        UserDto userUpdate = createUserUpdateOnExistingUser();

        var gatewayResponse = sendUpdateRequest(userUpdate.getUsername(), userUpdate);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_ACCEPTED)));
    }

    @DisplayName("handleRequest() returns BadRequest  when path contains an Id that is different from the Id"
                 + "of the input Object")
    @Test
    public void processInputReturnsBadRequestWhenPathContainsIdDifferentFromIdOfInputObject()
        throws ApiGatewayException, IOException {

        UserDto userUpdate = userUpdateOnExistingUser();
        UserDto anotherExistingUser = anotherUserInDatabase();
        String falseUsername = anotherExistingUser.getUsername();

        var gatewayResponse = sendUpdateRequest(falseUsername, userUpdate);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat(gatewayResponse.getBody(), containsString(INCONSISTENT_USERNAME_IN_PATH_AND_OBJECT_ERROR));
    }

    @DisplayName("handleRequest() returns BadRequest when input object is invalid")
    @Test
    void processInputReturnsBadRequestWhenInputObjectIsInvalid()
        throws ApiGatewayException, IOException {

        var existingUser = storeUserInDatabase(sampleUser());
        var userUpdate = createUserWithoutUsername();
        var gatewayResponse = sendUpdateRequest(existingUser.getUsername(), userUpdate);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat(gatewayResponse.getBody(), containsString(UserDto.USERNAME_FIELD));
    }

    @DisplayName("handleRequest() returns InternalServerError when handler is called without path parameter")
    @Test
    void processInputReturnsInternalServerErrorWhenHandlerIsCalledWithoutPathParameter()
        throws ApiGatewayException {

        UserDto userUpdate = createUserUpdateOnExistingUser();

        var gatewayResponse = sendUpdateRequestWithoutPathParameters(userUpdate);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
        assertThat(gatewayResponse.getBody(), containsString(ApiGatewayHandlerV2.INTERNAL_ERROR_MESSAGE));
    }

    @DisplayName("handleRequest() returns NotFound when trying to update non existing user")
    @Test
    public void processInputReturnsNotFoundWhenHandlerWhenTryingToUpdateNonExistingUser()
        throws IOException {

        UserDto nonExistingUser = sampleUser();
        var gatewayResponse = sendUpdateRequest(nonExistingUser.getUsername(), nonExistingUser);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_NOT_FOUND)));
        assertThat(gatewayResponse.getBody(), containsString(USER_NOT_FOUND_MESSAGE));
    }

    @Test
    @Disabled("Jackson Jr does not give us this possibility for now (2.13.1)")
    public void handleRequestReturnsBadRequestWhenInputUserHasNoType()
        throws InvalidEntryInternalException {

    }

    @ParameterizedTest
    @ValueSource(strings = {"##some?malformed?uri"})
    void shouldReturnBadRequestWhenInputViewingScopeContainsMalformedUris(String illegalUri)
        throws IOException {
        var userDto = sampleUser();
        var userJson = injectInvalidUriToViewingScope(illegalUri, userDto);
        var response = sendUpdateRequest(userDto.getUsername(), userJson);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));

        assertThat(response.getBody(), containsString(illegalUri));
    }

    private Map<String, Object> injectInvalidUriToViewingScope(String illegalUri, UserDto userDto)
        throws IOException {
        var userMap = objectMapper.mapFrom(userDto.toString());
        HashMap<Object, Object> viewingScope = creteViewingScopeNodeWithIllegalUri(illegalUri);
        userMap.put(VIEWING_SCOPE_FIELD, viewingScope);

        return userMap;
    }

    private HashMap<Object, Object> creteViewingScopeNodeWithIllegalUri(String illegalUri) {
        var includedUnits = new ArrayList<String>();
        includedUnits.add(illegalUri);
        var viewingScope = new HashMap<>();
        viewingScope.put(INCLUDED_UNITS, includedUnits);
        return viewingScope;
    }

    private UserDto anotherUserInDatabase()
        throws InvalidEntryInternalException, ConflictException, NotFoundException, InvalidInputException {
        UserDto anotherExistingUser = sampleUser().copy().withUsername(SOME_OTHER_USERNAME).build();
        storeUserInDatabase(anotherExistingUser);
        return anotherExistingUser;
    }

    private UserDto userUpdateOnExistingUser()
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException {
        UserDto existingUser = storeUserInDatabase(sampleUser());
        return createUserUpdate(existingUser);
    }

    private UserDto createUserUpdateOnExistingUser()
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException {
        UserDto existingUser = storeUserInDatabase(sampleUser());
        return createUserUpdate(existingUser);
    }

    private UserDto storeUserInDatabase(UserDto userDto)
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException {
        databaseService.addUser(userDto);
        return databaseService.getUser(userDto);
    }

    private <I> APIGatewayProxyResponseEvent sendUpdateRequest(String userId, I userUpdate)
        throws IOException {
        UpdateUserHandler updateUserHandler = new UpdateUserHandler(databaseService);
        String bodyString = userUpdate.toString();
        var input = new APIGatewayProxyRequestEvent()
            .withBody(bodyString)
            .withPathParameters(Collections.singletonMap(USERNAME_PATH_PARAMETER, userId))
            .withBody(bodyString);
        return updateUserHandler.handleRequest(input, context);
    }

    private APIGatewayProxyResponseEvent sendUpdateRequestWithoutPathParameters(UserDto userUpdate) {
        UpdateUserHandler updateUserHandler = new UpdateUserHandler(databaseService);
        var bodyString = attempt(() -> objectMapper.asString(userUpdate)).orElseThrow();
        var input = new APIGatewayProxyRequestEvent().withBody(bodyString);

        return updateUserHandler.handleRequest(input, context);
    }

    private UserDto sampleUser() throws InvalidEntryInternalException {
        RoleDto someRole = RoleDto.newBuilder().withRoleName(SAMPLE_ROLE).build();
        return UserDto.newBuilder()
            .withUsername(SAMPLE_USERNAME)
            .withInstitution(SAMPLE_INSTITUTION)
            .withRoles(Collections.singletonList(someRole))
            .withViewingScope(randomViewingScope())
            .build();
    }

    private UserDto createUserUpdate(UserDto userDto) throws InvalidEntryInternalException {
        RoleDto someOtherRole = RoleDto.newBuilder().withRoleName(ANOTHER_ROLE).build();
        return updateRoleList(userDto, someOtherRole);
    }

    private UserDto updateRoleList(UserDto userDto, RoleDto someOtherRole) throws InvalidEntryInternalException {
        return userDto.copy().withRoles(Collections.singletonList(someOtherRole)).build();
    }
}