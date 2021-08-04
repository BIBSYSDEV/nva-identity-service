package no.unit.nva.handlers;

import static no.unit.nva.handlers.ListByInstitutionHandler.INSTITUTION_ID_QUERY_PARAMETER;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.UserList;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ConflictException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ListByInstitutionHandlerTest extends HandlerTest {

    public static final String SOME_OTHER_USERNAME = "SomeOtherUsername";
    public static final String SOME_OTHER_INSTITUTION = "SomeOtherInstitution";
    private ListByInstitutionHandler listByInstitutionHandler;
    private Context context;

    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        listByInstitutionHandler = new ListByInstitutionHandler(mockEnvironment(), databaseService);
        context = mock(Context.class);
    }

    @Test
    public void handleRequestReturnsOkUponSuccessfulRequest() throws IOException {
        InputStream validRequest = createListRequest(DEFAULT_INSTITUTION);

        ByteArrayOutputStream output = sendRequestToHandler(validRequest);

        GatewayResponse<UserList> response = GatewayResponse.fromOutputStream(output);
        assertThatResponseIsSuccessful(response);
    }

    @Test
    public void handleRequestReturnsListOfUsersGivenAnInstitution()
        throws IOException, ConflictException, InvalidEntryInternalException, InvalidInputException {
        UserList expectedUsers = insertTwoUsersOfSameInstitution();

        InputStream validRequest = createListRequest(DEFAULT_INSTITUTION);
        ByteArrayOutputStream output = sendRequestToHandler(validRequest);

        GatewayResponse<UserList> response = GatewayResponse.fromOutputStream(output);
        assertThatResponseIsSuccessful(response);

        UserList actualUsers = response.getBodyObject(UserList.class);
        assertThatListsAreEquivalent(expectedUsers, actualUsers);
    }

    @Test
    public void handleRequestReturnsListOfUsersContainingOnlyUsersOfGivenInstitution()
        throws IOException, ConflictException, InvalidEntryInternalException, InvalidInputException {
        UserList insertedUsers = insertTwoUsersOfDifferentInstitutions();

        InputStream validRequest = createListRequest(DEFAULT_INSTITUTION);
        ByteArrayOutputStream output = sendRequestToHandler(validRequest);

        GatewayResponse<UserList> response = GatewayResponse.fromOutputStream(output);
        assertThatResponseIsSuccessful(response);

        UserList actualUsers = response.getBodyObject(UserList.class);
        UserList expectedUsers = expectedUsersOfInstitution(insertedUsers);
        assertThatListsAreEquivalent(expectedUsers, actualUsers);

        UserList unexpectedUsers = unexpectedUsers(insertedUsers, expectedUsers);
        assertThatActualResultDoesNotContainUnexpectedEntities(unexpectedUsers, actualUsers);
    }

    @Test
    public void handleRequestReturnsEmptyListOfUsersWhenNoUsersOfSpecifiedInstitutionAreFound()
        throws IOException, ConflictException, InvalidEntryInternalException, InvalidInputException {
        insertTwoUsersOfSameInstitution();

        InputStream validRequest = createListRequest(SOME_OTHER_INSTITUTION);
        ByteArrayOutputStream output = sendRequestToHandler(validRequest);

        GatewayResponse<UserList> response = GatewayResponse.fromOutputStream(output);
        assertThatResponseIsSuccessful(response);
        UserList actualUsers = response.getBodyObject(UserList.class);
        assertThat(actualUsers, is(empty()));
    }

    @Test
    public void processInputThrowsIllegalStateExceptionWhenPathParameterIsMissing() {
        RequestInfo requestInfo = new RequestInfo();

        Executable action = () -> listByInstitutionHandler.processInput(null, requestInfo, context);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), containsString(ListByInstitutionHandler.MISSING_QUERY_PARAMETER_ERROR));
    }

    private void assertThatResponseIsSuccessful(GatewayResponse<UserList> response) {
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
    }

    private UserList unexpectedUsers(UserList insertedUsers, UserList expectedUsers) {
        UserDto[] insertedUsersCopy = insertedUsers.toArray(UserDto[]::new);

        // use ArrayList explicitly because the List returned by the asList() method does not support removeAll
        UserList unexpectedUsers = UserList.fromList(new ArrayList<>(Arrays.asList(insertedUsersCopy)));
        unexpectedUsers.removeAll(expectedUsers);

        return unexpectedUsers;
    }

    private void assertThatActualResultDoesNotContainUnexpectedEntities(UserList actualUsers,
                                                                        UserList unexpectedUsers) {
        UserDto[] unexpectedUsersArray = new UserDto[unexpectedUsers.size()];
        unexpectedUsers.toArray(unexpectedUsersArray);
        assertThat(actualUsers, not(contains(unexpectedUsersArray)));
    }

    private UserList expectedUsersOfInstitution(UserList insertedUsers) {
        List<UserDto> users = insertedUsers.stream()
            .filter(userDto -> userDto.getInstitution().equals(HandlerTest.DEFAULT_INSTITUTION))
            .collect(Collectors.toList());
        return UserList.fromList(users);
    }

    private UserList insertTwoUsersOfDifferentInstitutions()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        UserList users = new UserList();
        users.add(insertSampleUserToDatabase(DEFAULT_USERNAME, HandlerTest.DEFAULT_INSTITUTION));
        users.add(insertSampleUserToDatabase(SOME_OTHER_USERNAME, SOME_OTHER_INSTITUTION));

        return users;
    }

    private ByteArrayOutputStream sendRequestToHandler(InputStream validRequest) throws IOException {
        ByteArrayOutputStream output = outputStream();
        listByInstitutionHandler.handleRequest(validRequest, output, context);
        return output;
    }

    private void assertThatListsAreEquivalent(UserList expectedUsers, UserList actualUsers) {
        assertThat(actualUsers, containsInAnyOrder(expectedUsers.toArray()));
        assertThat(expectedUsers, containsInAnyOrder(actualUsers.toArray()));
    }

    private UserList insertTwoUsersOfSameInstitution()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException {
        UserList users = new UserList();
        users.add(insertSampleUserToDatabase(DEFAULT_USERNAME, DEFAULT_INSTITUTION));
        users.add(insertSampleUserToDatabase(SOME_OTHER_USERNAME, DEFAULT_INSTITUTION));
        return users;
    }

    private ByteArrayOutputStream outputStream() {
        return new ByteArrayOutputStream();
    }

    private InputStream createListRequest(String institutionId) throws JsonProcessingException {
        Map<String, String> queryParams = Map.of(INSTITUTION_ID_QUERY_PARAMETER, institutionId);
        return new HandlerRequestBuilder<Void>(objectMapper)
            .withQueryParameters(queryParams)
            .build();
    }
}