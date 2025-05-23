package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.UserList;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.Environment;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.RandomUserDataGenerator.randomRoleNameButNot;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.handlers.ListByInstitutionHandler.INSTITUTION_ID_QUERY_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

class ListByInstitutionHandlerTest extends HandlerTest {

    public static final String SOME_OTHER_USERNAME = "SomeOtherUsername";
    public static final URI SOME_OTHER_INSTITUTION = randomCristinOrgId();
    private static final String NAME = "name";
    private static final String ROLE = "role";
    private ListByInstitutionHandler listByInstitutionHandler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        listByInstitutionHandler = new ListByInstitutionHandler(databaseService, new Environment());
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void handleRequestReturnsOkUponSuccessfulRequest() throws IOException {
        var validRequest = createListRequest(DEFAULT_INSTITUTION);

        var response = sendRequestToHandler(validRequest, UserList.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
    }

    private <T> GatewayResponse<T> sendRequestToHandler(InputStream validRequest, Class<T> responseBodyType)
        throws IOException {

        listByInstitutionHandler.handleRequest(validRequest, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseBodyType);
    }

    private InputStream createListRequest(URI institutionId) throws JsonProcessingException {
        Map<String, String> queryParams = Map.of(INSTITUTION_ID_QUERY_PARAMETER, institutionId.toString());
        return new HandlerRequestBuilder<>(dtoObjectMapper)
            .withQueryParameters(queryParams)
            .build();
    }

    @Test
    void handleRequestReturnsListOfUsersGivenAnInstitution()
        throws IOException, ConflictException, InvalidEntryInternalException {
        UserList expectedUsers = insertTwoUsersOfSameInstitution();

        var validRequest = createListRequest(DEFAULT_INSTITUTION);
        var response = sendRequestToHandler(validRequest, UserList.class);

        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
        var actualUsers = parseResponseBody(response);
        assertThatListsAreEquivalent(expectedUsers, actualUsers);
    }

    private UserList parseResponseBody(GatewayResponse<UserList> response) throws JsonProcessingException {
        return response.getBodyObject(UserList.class);
    }

    private void assertThatListsAreEquivalent(UserList expectedUsers, UserList actualUsers) {
        assertThat(actualUsers.getUsers(), containsInAnyOrder(expectedUsers.getUsers().toArray(UserDto[]::new)));
        assertThat(expectedUsers.getUsers(), containsInAnyOrder(actualUsers.getUsers().toArray()));
    }

    private UserList insertTwoUsersOfSameInstitution()
        throws InvalidEntryInternalException, ConflictException {
        UserList users = new UserList();
        users.getUsers().add(insertSampleUserToDatabase(DEFAULT_USERNAME, DEFAULT_INSTITUTION));
        users.getUsers().add(insertSampleUserToDatabase(SOME_OTHER_USERNAME, DEFAULT_INSTITUTION));
        return users;
    }

    @Test
    void handleRequestReturnsListOfUsersGivenAnInstitutionAndSingleRole()
        throws IOException, ConflictException, InvalidEntryInternalException {

        var name = randomRoleName();
        insertUserOfSameInstitution(DEFAULT_USERNAME, name);
        var expectedUser =
            insertUserOfSameInstitution(SOME_OTHER_USERNAME, randomRoleNameButNot(name)).getUsers().get(0);
        var roleName = ((RoleDto) expectedUser.getRoles().toArray()[0]).getRoleName();
        var validRequest = createListWithFilterRequest(DEFAULT_INSTITUTION, List.of(roleName));

        var response = sendRequestToHandler(validRequest, UserList.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));

        var actualUsers = parseResponseBody(response).getUsers();
        assertThat(actualUsers.get(0), is(equalTo(expectedUser)));
    }

    private UserList insertUserOfSameInstitution(String username, RoleName roleName)
        throws InvalidEntryInternalException, ConflictException {
        UserList users = new UserList();
        users.getUsers().add(insertSampleUserToDatabase(username, DEFAULT_INSTITUTION, roleName));
        return users;
    }

    private InputStream createListWithFilterRequest(URI institutionId, List<RoleName> roles)
        throws JsonProcessingException {
        var queryParams = Map.of(
            INSTITUTION_ID_QUERY_PARAMETER, institutionId.toString()
        );
        var multiValueParams = Map.of(ROLE, roles.stream().map(RoleName::getValue).toList());

        return new HandlerRequestBuilder<>(dtoObjectMapper)
            .withQueryParameters(queryParams)
            .withMultiValueQueryParameters(multiValueParams)
            .build();
    }

    @Test
    void handleRequestReturnsListOfUsersGivenAnInstitutionAndMultipleRoles()
        throws IOException, ConflictException, InvalidEntryInternalException {

        var user1 = insertSampleUserToDatabase(randomString(), DEFAULT_INSTITUTION, RoleName.SUPPORT_CURATOR);
        var user2 = insertSampleUserToDatabase(randomString(), DEFAULT_INSTITUTION, RoleName.SUPPORT_CURATOR);
        var user3 = insertSampleUserToDatabase(randomString(), DEFAULT_INSTITUTION, RoleName.DOI_CURATOR);
        var rolesOfFirstTwoUsers = List.of(
            user1.getRoles().stream().findFirst().get().getRoleName(),
            user2.getRoles().stream().findFirst().get().getRoleName()
        );

        var validRequest = createListWithFilterRequest(DEFAULT_INSTITUTION, rolesOfFirstTwoUsers);

        var response = sendRequestToHandler(validRequest, UserList.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));

        var actualUsers = parseResponseBody(response).getUsers();
        assertThat(actualUsers, hasItem(user1));
        assertThat(actualUsers, hasItem(user2));
        assertThat(actualUsers, not(hasItem(user3)));
    }

    @Test
    void handleRequestReturnsListOfUsersGivenAnInstitutionAndUserName()
        throws IOException, ConflictException, InvalidEntryInternalException {

        var expectedUser = insertTwoUsersOfSameInstitution().getUsers().get(0);
        var username = expectedUser.getUsername();
        var validRequest = createListWithFilterUserNameRequest(DEFAULT_INSTITUTION, username);

        var response = sendRequestToHandler(validRequest, UserList.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));

        var actualUsers = parseResponseBody(response).getUsers();
        assertThat(actualUsers.get(0), is(equalTo(expectedUser)));
    }

    private InputStream createListWithFilterUserNameRequest(URI institutionId, String username)
        throws JsonProcessingException {
        var queryParams = Map.of(
            INSTITUTION_ID_QUERY_PARAMETER, institutionId.toString(),
            NAME, username);

        return new HandlerRequestBuilder<>(dtoObjectMapper)
            .withQueryParameters(queryParams)
            .build();
    }

    @Test
    void handleRequestReturnsListOfUsersContainingOnlyUsersOfGivenInstitution()
        throws IOException, InvalidEntryInternalException, ConflictException {
        UserList insertedUsers = insertTwoUsersOfDifferentInstitutions();

        var validRequest = createListRequest(DEFAULT_INSTITUTION);
        var response = sendRequestToHandler(validRequest, UserList.class);

        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));

        var actualUsers = parseResponseBody(response);
        var expectedUsers = expectedUsersOfInstitution(insertedUsers);
        assertThatListsAreEquivalent(expectedUsers, actualUsers);

        var unexpectedUsers = unexpectedUsers(insertedUsers, expectedUsers);
        assertThatActualResultDoesNotContainUnexpectedEntities(unexpectedUsers, actualUsers);
    }

    private UserList unexpectedUsers(UserList insertedUsers, UserList expectedUsers) {
        UserDto[] insertedUsersCopy = insertedUsers.getUsers().toArray(UserDto[]::new);

        // use ArrayList explicitly because the List returned by the asList() method does not support removeAll
        UserList unexpectedUsers = UserList.fromList(new ArrayList<>(Arrays.asList(insertedUsersCopy)));
        unexpectedUsers.getUsers().removeAll(expectedUsers.getUsers());

        return unexpectedUsers;
    }

    private void assertThatActualResultDoesNotContainUnexpectedEntities(UserList actualUsers,
                                                                        UserList unexpectedUsers) {
        UserDto[] unexpectedUsersArray = new UserDto[unexpectedUsers.getUsers().size()];
        unexpectedUsers.getUsers().toArray(unexpectedUsersArray);
        assertThat(actualUsers.getUsers(), not(contains(unexpectedUsersArray)));
    }

    private UserList expectedUsersOfInstitution(UserList insertedUsers) {
        List<UserDto> users = insertedUsers.getUsers().stream()
            .filter(userDto -> userDto.getInstitution().equals(DEFAULT_INSTITUTION))
            .collect(Collectors.toList());
        return UserList.fromList(users);
    }

    private UserList insertTwoUsersOfDifferentInstitutions()
        throws InvalidEntryInternalException, ConflictException {
        UserList users = new UserList();
        users.getUsers().add(insertSampleUserToDatabase(DEFAULT_USERNAME, DEFAULT_INSTITUTION));
        users.getUsers().add(insertSampleUserToDatabase(SOME_OTHER_USERNAME, SOME_OTHER_INSTITUTION));

        return users;
    }

    @Test
    void handleRequestReturnsEmptyListOfUsersWhenNoUsersOfSpecifiedInstitutionAreFound()
        throws ConflictException, InvalidEntryInternalException, IOException {
        insertTwoUsersOfSameInstitution();

        var validRequest = createListRequest(SOME_OTHER_INSTITUTION);
        var response = sendRequestToHandler(validRequest, UserList.class);

        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
        UserList actualUserList = parseResponseBody(response);
        assertThat(actualUserList.getUsers(), is(empty()));
    }

    @Test
    void shouldReturnBadRequestWheRequestParameterIsMissing()
        throws InvalidEntryInternalException, IOException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper)
            .build();
        var response = sendRequestToHandler(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }
}