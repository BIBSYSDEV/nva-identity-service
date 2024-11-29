package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomRoleNameButNot;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.database.UserService.USER_NOT_FOUND_MESSAGE;
import static no.unit.nva.handlers.EntityUtils.createUserWithoutUsername;
import static no.unit.nva.handlers.UpdateUserHandler.INCONSISTENT_USERNAME_IN_PATH_AND_OBJECT_ERROR;
import static no.unit.nva.handlers.UpdateUserHandler.LOCATION_HEADER;
import static no.unit.nva.handlers.UpdateUserHandler.USERNAME_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.model.UserDto.VIEWING_SCOPE_FIELD;
import static no.unit.nva.useraccessservice.model.ViewingScope.INCLUDED_UNITS;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_AFFILIATION;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;

public class UpdateUserHandlerTest extends HandlerTest {

    public static final String SAMPLE_USERNAME = "some@somewhere";
    public static final URI SAMPLE_INSTITUTION = randomCristinOrgId();
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

    private UserDto sampleUser() throws InvalidEntryInternalException {
        var someRole = RoleDto.newBuilder().withRoleName(RoleName.CREATOR).build();
        return UserDto.newBuilder()
            .withUsername(SAMPLE_USERNAME)
            .withInstitution(SAMPLE_INSTITUTION)
            .withRoles(Collections.singletonList(someRole))
            .withViewingScope(randomViewingScope())
            .build();
    }

    private UserDto createUserUpdate(UserDto userDto) throws InvalidEntryInternalException {
        var anotherRoleName = userDto.getRoles().isEmpty()
            ? randomRoleNameButNot(RoleName.APPLICATION_ADMIN)
            : randomRoleNameButNot(userDto.getRoles().iterator().next().getRoleName());
        var someOtherRole =
            RoleDto.newBuilder().withRoleName(anotherRoleName).build();
        return userDto.copy()
            .withRoles(Collections.singletonList(someOtherRole))
            .withViewingScope(randomViewingScope())
            .build();
    }

    private <I, O> GatewayResponse<O> sendUpdateRequest(String userId, I userUpdate,
                                                        Class<O> responseType)
        throws IOException {
        var updateUserHandler = new UpdateUserHandler(databaseService);
        var customerId = randomUri();
        var input = new HandlerRequestBuilder<I>(dtoObjectMapper)
            .withBody(userUpdate)
            .withCurrentCustomer(customerId)
            .withAccessRights(customerId, MANAGE_CUSTOMERS)
            .withPathParameters(Collections.singletonMap(USERNAME_PATH_PARAMETER, userId))
            .build();
        updateUserHandler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
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

        var userUpdate = createUserUpdateOnExistingUser();

        var gatewayResponse = sendUpdateRequestWithoutPathParameters(userUpdate);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
        assertThat(gatewayResponse.getBody(),
            containsString(MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    private GatewayResponse<Problem> sendUpdateRequestWithoutPathParameters(UserDto userUpdate)
        throws IOException {
        var customerId = randomUri();
        var updateUserHandler = new UpdateUserHandler(databaseService);
        var input = new HandlerRequestBuilder<UserDto>(dtoObjectMapper)
            .withCurrentCustomer(customerId)
            .withAccessRights(customerId, MANAGE_OWN_AFFILIATION)
            .withBody(userUpdate)
            .build();

        updateUserHandler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, Problem.class);
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
        viewingScope.put("type", "ViewingScope");
        return viewingScope;
    }

    @Test
    void shouldDenyAccessForUserWhenItDoesNotHaveAccessRights() throws IOException, ConflictException,
        NotFoundException {
        var userUpdate = createUserUpdateForElevatingRole();
        var gatewayResponse = sendUpdateRequestWithoutAccessRights(userUpdate.getUsername(), userUpdate, UserDto.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_FORBIDDEN)));
    }

    private UserDto createUserUpdateForElevatingRole()
        throws ConflictException, InvalidEntryInternalException, NotFoundException {
        UserDto existingUser = storeUserInDatabase(sampleUser());
        return createUserUpdateAppAdmin(existingUser);
    }

    private UserDto createUserUpdateAppAdmin(UserDto userDto) throws InvalidEntryInternalException {
        var appAdminRole = getAppAdminRole();
        return userDto.copy()
            .withRoles(Collections.singletonList(appAdminRole))
            .withViewingScope(randomViewingScope())
            .build();
    }

    private static RoleDto getAppAdminRole() {
        return RoleDto.newBuilder().withRoleName(RoleName.APPLICATION_ADMIN).build();
    }

    private <I, O> GatewayResponse<O> sendUpdateRequestWithoutAccessRights(String userId, I userUpdate,
                                                                           Class<O> responseType)
        throws IOException {
        var updateUserHandler = new UpdateUserHandler(databaseService);
        var input = new HandlerRequestBuilder<I>(dtoObjectMapper)
            .withBody(userUpdate)
            .withPathParameters(Collections.singletonMap(USERNAME_PATH_PARAMETER, userId))
            .build();
        updateUserHandler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    @Test
    void shouldDenyAccessWhenInstitutionAdminTriesToCreateAnAppAdmin()
        throws IOException, ConflictException, NotFoundException {
        var userUpdate = createUserUpdateForElevatingRole();
        var gatewayResponse = sendUpdateRequestForInstitutionAdmin(userUpdate.getUsername(), userUpdate, UserDto.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_FORBIDDEN)));
    }

    private <I, O> GatewayResponse<O> sendUpdateRequestForInstitutionAdmin(String userId, I userUpdate,
                                                                           Class<O> responseType)
        throws IOException {
        var updateUserHandler = new UpdateUserHandler(databaseService);
        var customerId = randomUri();
        var input = new HandlerRequestBuilder<I>(dtoObjectMapper)
            .withBody(userUpdate)
            .withCurrentCustomer(customerId)
            .withAccessRights(customerId, MANAGE_OWN_AFFILIATION)
            .withPathParameters(Collections.singletonMap(USERNAME_PATH_PARAMETER, userId))
            .build();
        updateUserHandler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    @Test
    void shouldDenyAccessWhenInstitutionAdminTriesToRemoveAppAdmin()
        throws ConflictException, NotFoundException, IOException, InvalidInputException {
        databaseService.addRole(getAppAdminRole());
        var existingUser = storeUserInDatabase(sampleUserWithRoles(List.of(getAppAdminRole())));
        var userUpdate = createUserUpdateRemoveAllRoles(existingUser);

        var gatewayResponse = sendUpdateRequestForInstitutionAdmin(userUpdate.getUsername(), userUpdate, UserDto.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_FORBIDDEN)));
    }

    private UserDto sampleUserWithRoles(List<RoleDto> roleDtos) throws InvalidEntryInternalException {
        return UserDto.newBuilder()
            .withUsername(SAMPLE_USERNAME)
            .withInstitution(SAMPLE_INSTITUTION)
            .withRoles(roleDtos)
            .withViewingScope(randomViewingScope())
            .build();
    }

    private UserDto createUserUpdateRemoveAllRoles(UserDto userDto) {
        return userDto.copy()
            .withRoles(Collections.emptyList())
            .withViewingScope(randomViewingScope())
            .build();
    }

    @Test
    void shouldAllowAccessWhenAppAdminTriesToCreateAnAppAdmin() throws IOException, ConflictException,
        NotFoundException {
        var userUpdate = createUserUpdateForElevatingRole();
        var gatewayResponse = sendUpdateRequest(userUpdate.getUsername(), userUpdate, UserDto.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_ACCEPTED)));
    }

    @Test
    void shouldAllowAccessWhenInstitutionAdminTriesToEditAnAppAdminsOtherRoles()
        throws InvalidInputException, ConflictException, NotFoundException, IOException {

        var adminRole = getAppAdminRole();
        var otherRole = getRandomRole();

        databaseService.addRole(adminRole);
        databaseService.addRole(otherRole);

        var existingUser = storeUserInDatabase(sampleUserWithRoles(List.of(adminRole, otherRole)));
        var userUpdate = createUserUpdateWithRoles(existingUser, List.of(adminRole));

        var gatewayResponse = sendUpdateRequestForInstitutionAdmin(userUpdate.getUsername(), userUpdate, UserDto.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpStatus.SC_ACCEPTED)));
    }

    private static RoleDto getRandomRole() {
        return RoleDto.newBuilder().withRoleName(randomRoleNameButNot(RoleName.APPLICATION_ADMIN)).build();
    }

    private UserDto createUserUpdateWithRoles(UserDto userDto, List<RoleDto> roles) {
        return userDto.copy()
            .withRoles(roles)
            .withViewingScope(randomViewingScope())
            .build();

    }

}