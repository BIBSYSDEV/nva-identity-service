package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.handlers.ListByInstitutionHandler.INSTITUTION_ID_QUERY_PARAMETER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.UserList;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

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
        listByInstitutionHandler = new ListByInstitutionHandler(databaseService);
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void handleRequestReturnsOkUponSuccessfulRequest() throws IOException {
        var validRequest = createListRequest(DEFAULT_INSTITUTION);

        var response = sendRequestToHandler(validRequest, UserList.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
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

    @Test
    void handleRequestReturnsListOfUsersGivenAnInstitutionAndRole()
        throws IOException, ConflictException, InvalidEntryInternalException {

        var expectedUser = insertTwoUsersOfSameInstitution().getUsers().get(0);
        var roleName = ((RoleDto) expectedUser.getRoles().toArray()[0]).getRoleName();
        var validRequest = createListWithFilterRequest(DEFAULT_INSTITUTION, roleName);

        var response = sendRequestToHandler(validRequest, UserList.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));

        var actualUsers = parseResponseBody(response).getUsers();
        assertThat(actualUsers.get(0), is(equalTo(expectedUser)));
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

    private UserList parseResponseBody(GatewayResponse<UserList> response) throws JsonProcessingException {
        return response.getBodyObject(UserList.class);
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
                                  .filter(userDto -> userDto.getInstitution().equals(HandlerTest.DEFAULT_INSTITUTION))
                                  .collect(Collectors.toList());
        return UserList.fromList(users);
    }

    private UserList insertTwoUsersOfDifferentInstitutions()
        throws InvalidEntryInternalException, ConflictException {
        UserList users = new UserList();
        users.getUsers().add(insertSampleUserToDatabase(DEFAULT_USERNAME, HandlerTest.DEFAULT_INSTITUTION));
        users.getUsers().add(insertSampleUserToDatabase(SOME_OTHER_USERNAME, SOME_OTHER_INSTITUTION));

        return users;
    }

    private <T> GatewayResponse<T> sendRequestToHandler(InputStream validRequest, Class<T> responseBodyType)
        throws IOException {

        listByInstitutionHandler.handleRequest(validRequest, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseBodyType);
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

    private InputStream createListRequest(URI institutionId) throws JsonProcessingException {
        Map<String, String> queryParams = Map.of(INSTITUTION_ID_QUERY_PARAMETER, institutionId.toString());
        return new HandlerRequestBuilder<>(dtoObjectMapper)
                   .withQueryParameters(queryParams)
                   .build();
    }

    private InputStream createListWithFilterRequest(URI institutionId, String role) throws JsonProcessingException {
        var queryParams = Map.of(
            INSTITUTION_ID_QUERY_PARAMETER, institutionId.toString(),
            ROLE, role);

        return new HandlerRequestBuilder<>(dtoObjectMapper)
                   .withQueryParameters(queryParams)
                   .build();
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
}