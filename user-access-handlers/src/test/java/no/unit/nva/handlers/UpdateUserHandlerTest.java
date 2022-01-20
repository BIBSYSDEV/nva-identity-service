package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.database.UserService.USER_NOT_FOUND_MESSAGE;
import static no.unit.nva.handlers.EntityUtils.createUserWithoutUsername;
import static no.unit.nva.handlers.UpdateUserHandler.INCONSISTENT_USERNAME_IN_PATH_AND_OBJECT_ERROR;
import static no.unit.nva.handlers.UpdateUserHandler.LOCATION_HEADER;
import static no.unit.nva.handlers.UpdateUserHandler.USERNAME_PATH_PARAMETER;
import static no.unit.nva.useraccessmanagement.RestConfig.defaultRestObjectMapper;
import static no.unit.nva.useraccessmanagement.model.UserDto.VIEWING_SCOPE_FIELD;
import static no.unit.nva.useraccessmanagement.model.ViewingScope.INCLUDED_UNITS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;
import no.unit.nva.Constants;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;

public class UpdateUserHandlerTest extends HandlerTest {

    public static final String SAMPLE_ROLE = "someRole";
    public static final String SAMPLE_USERNAME = "some@somewhere";
    public static final String SAMPLE_INSTITUTION = "somewhere";
    public static final String ANOTHER_ROLE = "ANOTHER_ROLE";
    public static final String SOME_OTHER_USERNAME = "SomeOtherUsername";
    private IdentityServiceImpl databaseService;
    private Context context;

    private ByteArrayOutputStream output;
    private static final Environment ENVIRONMENT = new Environment();

    @BeforeEach
    public void init() {
        databaseService = new IdentityServiceImpl(initializeTestDatabase());
        context = mock(Context.class);
        output = new ByteArrayOutputStream();
    }

    @DisplayName("handleRequest() returns Location header with the URI to updated user when path contains "
                 + "an existing user id, and the body contains a valid UserDto and path id is the same as the body id ")
    @Test
    public void handleRequestReturnsUpdatedUserWhenPathAndBodyContainTheSameUserIdAndTheIdExistsAndBodyIsValid()
        throws ApiGatewayException, IOException {

        UserDto userUpdate = createUserUpdateOnExistingUser();

        GatewayResponse<Void> gatewayResponse = sendUpdateRequest(userUpdate.getUsername(), userUpdate);
        Map<String, String> responseHeaders = gatewayResponse.getHeaders();

        assertThat(responseHeaders, hasKey(LOCATION_HEADER));

        String expectedLocationPath = UpdateUserHandler.USERS_RELATIVE_PATH + userUpdate.getUsername();
        assertThat(responseHeaders.get(LOCATION_HEADER), is(equalTo(expectedLocationPath)));
    }

    @DisplayName("handleRequest() can process URL encoded usernames")
    @Test
    public void handleRequestCanProcessUrlEncodedUsernames()
        throws ApiGatewayException, IOException {

        UserDto userUpdate = createUserUpdateOnExistingUser();

        String encodedUsername = encodeString(userUpdate.getUsername());
        GatewayResponse<Void> gatewayResponse = sendUpdateRequest(encodedUsername, userUpdate);
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

        GatewayResponse<Void> gatewayResponse = sendUpdateRequest(userUpdate.getUsername(), userUpdate);

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

        GatewayResponse<Problem> gatewayResponse = sendUpdateRequest(falseUsername, userUpdate);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));

        Problem problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(INCONSISTENT_USERNAME_IN_PATH_AND_OBJECT_ERROR));
    }

    @DisplayName("handleRequest() returns BadRequest when input object is invalid")
    @Test
    public void processInputReturnsBadRequestWhenInputObjectIsInvalid()
        throws ApiGatewayException, IOException {

        UserDto existingUser = storeUserInDatabase(sampleUser());
        ObjectNode userUpdate = createUserWithoutUsername();

        GatewayResponse<Problem> gatewayResponse = sendUpdateRequest(existingUser.getUsername(), userUpdate);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));

        Problem problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(UserDto.USERNAME_FIELD));
    }

    @DisplayName("handleRequest() returns InternalServerError when handler is called without path parameter")
    @Test
    public void processInputReturnsInternalServerErrorWhenHandlerIsCalledWithoutPathParameter()
        throws ApiGatewayException, IOException {

        UserDto userUpdate = createUserUpdateOnExistingUser();

        GatewayResponse<Problem> gatewayResponse = sendUpdateRequestWithoutPathParameters(userUpdate);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

        Problem problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(problem.getDetail(),
                   containsString(
                       ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @DisplayName("handleRequest() returns NotFound when trying to update non existing user")
    @Test
    public void processInputReturnsNotFoundWhenHandlerWhenTryingToUpdateNonExistingUser()
        throws ApiGatewayException, IOException {

        UserDto nonExistingUser = sampleUser();
        GatewayResponse<Problem> gatewayResponse = sendUpdateRequest(nonExistingUser.getUsername(), nonExistingUser);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_NOT_FOUND)));

        Problem problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(USER_NOT_FOUND_MESSAGE));
    }

    @Test
    public void handleRequestReturnsBadRequestWhenInputUserHasNoType()
        throws InvalidEntryInternalException, IOException, BadRequestException {

        UserDto userDto = sampleUser();
        ObjectNode objectWithoutType = inputObjectWithoutType(userDto);

        GatewayResponse<Problem> response = sendUpdateRequest(userDto.getUsername(), objectWithoutType);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));

        Problem problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(Constants.COULD_NOT_RESOLVE_SUBTYPE_OF));
    }

    @ParameterizedTest
    @ValueSource(strings = {"##some?malformed?uri", "https://www.example.com/194.63.0.0"})
    void shouldReturnBadRequestWhenInputViewingScopeContainsMalformedUris(String illegalUri)
        throws IOException, BadRequestException {
        var userDto = sampleUser();
        var userJson = injectInvalidUriToViewingScope(illegalUri, userDto);
        GatewayResponse<Problem> response = sendUpdateRequest(userDto.getUsername(), userJson);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(illegalUri));
    }

    private ObjectNode injectInvalidUriToViewingScope(String illegalUri, UserDto userDto)
        throws JsonProcessingException {
        var jsonString = JsonUtils.dtoObjectMapper.writeValueAsString(userDto);
        var userJson = (ObjectNode) JsonUtils.dtoObjectMapper.readTree(jsonString);
        var includedUrisNode = JsonUtils.dtoObjectMapper.createArrayNode();
        includedUrisNode.add(illegalUri);
        var viewingScopeNode = (ObjectNode) userJson.get(VIEWING_SCOPE_FIELD);
        viewingScopeNode.set(INCLUDED_UNITS, includedUrisNode);
        return userJson;
    }

    private UserDto anotherUserInDatabase()
        throws InvalidEntryInternalException, ConflictException, NotFoundException, InvalidInputException,
               BadRequestException {
        UserDto anotherExistingUser = sampleUser().copy().withUsername(SOME_OTHER_USERNAME).build();
        storeUserInDatabase(anotherExistingUser);
        return anotherExistingUser;
    }

    private UserDto userUpdateOnExistingUser()
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException,
               BadRequestException {
        UserDto existingUser = storeUserInDatabase(sampleUser());
        return createUserUpdate(existingUser);
    }

    private UserDto createUserUpdateOnExistingUser()
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException,
               BadRequestException {
        UserDto existingUser = storeUserInDatabase(sampleUser());
        return createUserUpdate(existingUser);
    }

    private ObjectNode inputObjectWithoutType(UserDto userDto) {
        ObjectNode objectWithoutType = defaultRestObjectMapper.convertValue(userDto, ObjectNode.class);
        objectWithoutType.remove(TYPE_ATTRIBUTE);
        return objectWithoutType;
    }

    private UserDto storeUserInDatabase(UserDto userDto)
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException {
        databaseService.addUser(userDto);
        return databaseService.getUser(userDto);
    }

    private <I, O> GatewayResponse<O> sendUpdateRequest(String userId, I userUpdate)
        throws IOException {
        UpdateUserHandler updateUserHandler = new UpdateUserHandler(ENVIRONMENT, databaseService);
        InputStream input = new HandlerRequestBuilder<I>(defaultRestObjectMapper)
            .withPathParameters(Collections.singletonMap(USERNAME_PATH_PARAMETER, userId))
            .withBody(userUpdate)
            .build();
        updateUserHandler.handleRequest(input, output, context);
        return GatewayResponse.fromOutputStream(output);
    }

    private GatewayResponse<Problem> sendUpdateRequestWithoutPathParameters(UserDto userUpdate)
        throws IOException {
        UpdateUserHandler updateUserHandler = new UpdateUserHandler(ENVIRONMENT, databaseService);
        InputStream input = new HandlerRequestBuilder<UserDto>(defaultRestObjectMapper)
            .withBody(userUpdate)
            .build();
        updateUserHandler.handleRequest(input, output, context);
        return GatewayResponse.fromOutputStream(output);
    }

    private UserDto sampleUser() throws InvalidEntryInternalException, BadRequestException {
        RoleDto someRole = RoleDto.newBuilder().withName(SAMPLE_ROLE).build();
        return UserDto.newBuilder()
            .withUsername(SAMPLE_USERNAME)
            .withInstitution(SAMPLE_INSTITUTION)
            .withRoles(Collections.singletonList(someRole))
            .withViewingScope(randomViewingScope())
            .build();
    }

    private UserDto createUserUpdate(UserDto userDto) throws InvalidEntryInternalException {
        RoleDto someOtherRole = RoleDto.newBuilder().withName(ANOTHER_ROLE).build();
        return updateRoleList(userDto, someOtherRole);
    }

    private UserDto updateRoleList(UserDto userDto, RoleDto someOtherRole) throws InvalidEntryInternalException {
        return userDto.copy().withRoles(Collections.singletonList(someOtherRole)).build();
    }
}