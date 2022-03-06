package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.handlers.ListByInstitutionHandler.INSTITUTION_ID_QUERY_PARAMETER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.UserList;
import nva.commons.apigatewayv2.exceptions.ConflictException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListByInstitutionHandlerTest extends HandlerTest {

    public static final String SOME_OTHER_USERNAME = "SomeOtherUsername";
    public static final URI SOME_OTHER_INSTITUTION = randomCristinOrgId();
    private ListByInstitutionHandler listByInstitutionHandler;
    private Context context;

    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        listByInstitutionHandler = new ListByInstitutionHandler(databaseService);
        context = mock(Context.class);
    }

    @Test
    void handleRequestReturnsOkUponSuccessfulRequest() {
        var validRequest = createListRequest(DEFAULT_INSTITUTION);

        var response = sendRequestToHandler(validRequest);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
    }

    @Test
    void handleRequestReturnsListOfUsersGivenAnInstitution()
        throws IOException, ConflictException, InvalidEntryInternalException, InvalidInputException {
        UserList expectedUsers = insertTwoUsersOfSameInstitution();

        var validRequest = createListRequest(DEFAULT_INSTITUTION);
        var response = sendRequestToHandler(validRequest);

        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
        var actualUsers = parseResponseBody(response);
        assertThatListsAreEquivalent(expectedUsers, actualUsers);
    }

    @Test
    void handleRequestReturnsListOfUsersContainingOnlyUsersOfGivenInstitution()
        throws IOException, ConflictException, InvalidEntryInternalException, InvalidInputException {
        UserList insertedUsers = insertTwoUsersOfDifferentInstitutions();

        var validRequest = createListRequest(DEFAULT_INSTITUTION);
        var response = sendRequestToHandler(validRequest);

        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));

        var actualUsers = parseResponseBody(response);
        var expectedUsers = expectedUsersOfInstitution(insertedUsers);
        assertThatListsAreEquivalent(expectedUsers, actualUsers);

        var unexpectedUsers = unexpectedUsers(insertedUsers, expectedUsers);
        assertThatActualResultDoesNotContainUnexpectedEntities(unexpectedUsers, actualUsers);
    }

    @Test
    void handleRequestReturnsEmptyListOfUsersWhenNoUsersOfSpecifiedInstitutionAreFound()
        throws IOException, ConflictException, InvalidEntryInternalException, InvalidInputException {
        insertTwoUsersOfSameInstitution();

        var validRequest = createListRequest(SOME_OTHER_INSTITUTION);
        var response = sendRequestToHandler(validRequest);

        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
        UserList actualUserList = parseResponseBody(response);
        assertThat(actualUserList.getUsers(), is(empty()));
    }

    @Test
    void shouldReturnBadRequestWheRequestParameterIsMissing() throws InvalidEntryInternalException {
        var request = new APIGatewayProxyRequestEvent();
        var response = listByInstitutionHandler.handleRequest(request, context);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    private UserList parseResponseBody(APIGatewayProxyResponseEvent response) {
        return UserList.fromJson(response.getBody());
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
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        UserList users = new UserList();
        users.getUsers().add(insertSampleUserToDatabase(DEFAULT_USERNAME, HandlerTest.DEFAULT_INSTITUTION));
        users.getUsers().add(insertSampleUserToDatabase(SOME_OTHER_USERNAME, SOME_OTHER_INSTITUTION));

        return users;
    }

    private APIGatewayProxyResponseEvent sendRequestToHandler(APIGatewayProxyRequestEvent validRequest) {

        return listByInstitutionHandler.handleRequest(validRequest, context);
    }

    private void assertThatListsAreEquivalent(UserList expectedUsers, UserList actualUsers) {
        assertThat(actualUsers.getUsers(), containsInAnyOrder(expectedUsers.getUsers().toArray(UserDto[]::new)));
        assertThat(expectedUsers.getUsers(), containsInAnyOrder(actualUsers.getUsers().toArray()));
    }

    private UserList insertTwoUsersOfSameInstitution()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException {
        UserList users = new UserList();
        users.getUsers().add(insertSampleUserToDatabase(DEFAULT_USERNAME, DEFAULT_INSTITUTION));
        users.getUsers().add(insertSampleUserToDatabase(SOME_OTHER_USERNAME, DEFAULT_INSTITUTION));
        return users;
    }

    private APIGatewayProxyRequestEvent createListRequest(URI institutionId) {
        Map<String, String> queryParams = Map.of(INSTITUTION_ID_QUERY_PARAMETER, institutionId.toString());
        return new APIGatewayProxyRequestEvent().withQueryStringParameters(queryParams);
    }
}