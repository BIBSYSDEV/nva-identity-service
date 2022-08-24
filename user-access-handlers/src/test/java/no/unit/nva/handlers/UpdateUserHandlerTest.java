package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.database.UserService.USER_NOT_FOUND_MESSAGE;
import static no.unit.nva.handlers.EntityUtils.createUserWithoutUsername;
import static no.unit.nva.handlers.UpdateUserHandler.INCONSISTENT_USERNAME_IN_PATH_AND_OBJECT_ERROR;
import static no.unit.nva.handlers.UpdateUserHandler.LOCATION_HEADER;
import static no.unit.nva.handlers.UpdateUserHandler.USERNAME_PATH_PARAMETER;
import static no.unit.nva.useraccessservice.model.UserDto.VIEWING_SCOPE_FIELD;
import static no.unit.nva.useraccessservice.model.ViewingScope.INCLUDED_UNITS;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
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
    public static final URI SAMPLE_INSTITUTION = randomCristinOrgId();
    
    public static final String ANOTHER_ROLE = "ANOTHER_ROLE";
    public static final String SOME_OTHER_USERNAME = "SomeOtherUsername";
    private IdentityServiceImpl databaseService;
    private Context context;
    private ByteArrayOutputStream outputStream;
    
    @BeforeEach
    public void init() {
        databaseService = new IdentityServiceImpl(initializeTestDatabase());
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }
    
    @DisplayName("handleRequest() returns 202 (Accepted)  when path contains "
                 + "an existing user id, and the body contains a valid UserDto and path id is the same as the body id ")
    @Test
    public void processInputReturnsAcceptedWhenPathAndBodyContainTheSameUserIdAndTheIdExistsAndBodyIsValid()
        throws ApiGatewayException, IOException {
        
        UserDto userUpdate = createUserUpdateOnExistingUser();
        
        var gatewayResponse = sendUpdateRequest(userUpdate.getUsername(), userUpdate, UserDto.class);
        
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
        var gatewayResponse = sendUpdateRequest(falseUsername, userUpdate, Problem.class);
        
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat(gatewayResponse.getBody(), containsString(INCONSISTENT_USERNAME_IN_PATH_AND_OBJECT_ERROR));
    }
    
    @DisplayName("handleRequest() returns NotFound when trying to update non existing user")
    @Test
    public void processInputReturnsNotFoundWhenHandlerWhenTryingToUpdateNonExistingUser()
        throws IOException {
        
        UserDto nonExistingUser = sampleUser();
        var gatewayResponse =
            sendUpdateRequest(nonExistingUser.getUsername(), nonExistingUser, Problem.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_NOT_FOUND)));
        assertThat(gatewayResponse.getBody(), containsString(USER_NOT_FOUND_MESSAGE));
    }
    
    @DisplayName("handleRequest() returns Location header with the URI to updated user when path contains "
                 + "an existing user id, and the body contains a valid UserDto and path id is the same as the body id ")
    @Test
    void handleRequestReturnsUpdatedUserWhenPathAndBodyContainTheSameUserIdAndTheIdExistsAndBodyIsValid()
        throws ApiGatewayException, IOException {
        
        UserDto userUpdate = createUserUpdateOnExistingUser();
        var gatewayResponse = sendUpdateRequest(userUpdate.getUsername(), userUpdate, UserDto.class);
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
        var gatewayResponse = sendUpdateRequest(encodedUsername, userUpdate, UserDto.class);
        Map<String, String> responseHeaders = gatewayResponse.getHeaders();
        
        assertThat(responseHeaders, hasKey(LOCATION_HEADER));
        
        String expectedLocationPath = UpdateUserHandler.USERS_RELATIVE_PATH + userUpdate.getUsername();
        assertThat(responseHeaders.get(LOCATION_HEADER), is(equalTo(expectedLocationPath)));
    }
    
    @DisplayName("handleRequest() returns BadRequest when input object is invalid")
    @Test
    void processInputReturnsBadRequestWhenInputObjectIsInvalid()
        throws ApiGatewayException, IOException {
        
        var existingUser = storeUserInDatabase(sampleUser());
        var userUpdate = createUserWithoutUsername();
        var gatewayResponse = sendUpdateRequest(existingUser.getUsername(), userUpdate, Problem.class);
        
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat(gatewayResponse.getBody(), containsString(UserDto.USERNAME_FIELD));
    }
    
    @DisplayName("handleRequest() returns InternalServerError when handler is called without path parameter")
    @Test
    void processInputReturnsInternalServerErrorWhenHandlerIsCalledWithoutPathParameter()
        throws ApiGatewayException, IOException {
        
        UserDto userUpdate = createUserUpdateOnExistingUser();
        
        var gatewayResponse = sendUpdateRequestWithoutPathParameters(userUpdate);
        
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
        assertThat(gatewayResponse.getBody(),
            containsString(MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"##some?malformed?uri"})
    void shouldReturnBadRequestWhenInputViewingScopeContainsMalformedUris(String illegalUri)
        throws IOException {
        var userDto = sampleUser();
        var userJson = injectInvalidUriToViewingScope(illegalUri, userDto);
        var response = sendUpdateRequest(userDto.getUsername(), userJson, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        
        assertThat(response.getBody(), containsString(illegalUri));
    }
    
    private Map<String, Object> injectInvalidUriToViewingScope(String illegalUri, UserDto userDto)
        throws IOException {
        var userMap = JsonConfig.mapFrom(userDto.toString());
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
        throws InvalidEntryInternalException, ConflictException, NotFoundException {
        UserDto anotherExistingUser = sampleUser().copy().withUsername(SOME_OTHER_USERNAME).build();
        storeUserInDatabase(anotherExistingUser);
        return anotherExistingUser;
    }
    
    private UserDto userUpdateOnExistingUser()
        throws ConflictException, InvalidEntryInternalException, NotFoundException {
        UserDto existingUser = storeUserInDatabase(sampleUser());
        return createUserUpdate(existingUser);
    }
    
    private UserDto createUserUpdateOnExistingUser()
        throws ConflictException, InvalidEntryInternalException, NotFoundException {
        UserDto existingUser = storeUserInDatabase(sampleUser());
        return createUserUpdate(existingUser);
    }
    
    private UserDto storeUserInDatabase(UserDto userDto)
        throws ConflictException, InvalidEntryInternalException, NotFoundException {
        databaseService.addUser(userDto);
        return databaseService.getUser(userDto);
    }
    
    private <I, O> GatewayResponse<O> sendUpdateRequest(String userId, I userUpdate, Class<O> responseType)
        throws IOException {
        var updateUserHandler = new UpdateUserHandler(databaseService);
        var input = new HandlerRequestBuilder<I>(dtoObjectMapper)
            .withBody(userUpdate)
            .withPathParameters(Collections.singletonMap(USERNAME_PATH_PARAMETER, userId))
            .build();
        updateUserHandler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }
    
    private GatewayResponse<Problem> sendUpdateRequestWithoutPathParameters(UserDto userUpdate)
        throws IOException {
        var updateUserHandler = new UpdateUserHandler(databaseService);
        var input = new HandlerRequestBuilder<UserDto>(dtoObjectMapper)
            .withBody(userUpdate)
            .build();
        
        updateUserHandler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, Problem.class);
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
        return userDto.copy()
            .withRoles(Collections.singletonList(someOtherRole))
            .withViewingScope(randomViewingScope())
            .build();
    }
    
}