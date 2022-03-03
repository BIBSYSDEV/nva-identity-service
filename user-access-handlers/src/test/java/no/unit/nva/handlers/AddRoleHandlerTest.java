package no.unit.nva.handlers;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.RoleService;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddRoleHandlerTest extends HandlerTest {

    public static final String SOME_ROLE_NAME = "someRoleName";
    private RoleDto sampleRole;
    private AddRoleHandler addRoleHandler;
    private Context context;

    /**
     * init.
     *
     * @throws InvalidEntryInternalException when an invalid role is created
     */
    @BeforeEach
    public void init() throws InvalidEntryInternalException {
        context = mock(Context.class);
        IdentityService service = new IdentityServiceImpl(initializeTestDatabase());
        addRoleHandler = new AddRoleHandler(service);
        sampleRole = sampleRole();
    }

    @Test
    void handleRequestReturnsBadRequestWhenRequestBodyIsEmpty() {
        var response = sendRequest(null);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    void handleRequestReturnsBadRequestWhenRequestBodyIsAnEmptyObject() {
        var response = sendRequest(new RoleDto());
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    void handlerRequestReturnsOkWheRequestBodyIsValid() throws InvalidEntryInternalException {
        var response = sendRequest(sampleRole());
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_OK)));
    }

    @Test
    void shouldReturnBadRequestWheRequestBodyIsInValid() throws InvalidEntryInternalException {
        var request = new APIGatewayProxyRequestEvent().withBody(randomString());
        var response = addRoleHandler.handleRequest(request, context);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    void handlerRequestReturnsTheGeneratedObjectWhenInputIsValid()
        throws InvalidEntryInternalException, IOException {
        var actualRole = sampleRole();
        var response = sendRequest(actualRole);
        RoleDto savedRole = extractResponseBody(response);
        assertThat(savedRole, is(equalTo(actualRole)));
    }

    @Test
    void handlerRequestReturnsTheGeneratedObjectAfterWaitingForSyncingToComplete()
        throws InvalidEntryInternalException, IOException {
        var actualRole = sampleRole();
        var service = databaseServiceWithSyncDelay();
        addRoleHandler = new AddRoleHandler(service);
        var response = sendRequest(actualRole);
        var savedRole = extractResponseBody(response);
        assertThat(savedRole, is(equalTo(actualRole)));
    }

    @Test
    void handleRequestReturnsInternalServerErrorWhenDatabaseFailsToSaveTheData()
        throws InvalidEntryInternalException {
        RoleDto actualRole = sampleRole();
        IdentityService service = databaseServiceAddingButNotGettingARole();
        addRoleHandler = new AddRoleHandler(service);

        var response = sendRequest(actualRole);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
        assertThat(response.getBody(), containsString(AddRoleHandler.ERROR_FETCHING_SAVED_ROLE));
    }

    @Test
    void statusCodeReturnsOkWhenRequestIsSuccessful() {
        Integer successCode = addRoleHandler.getSuccessStatusCode(null, null);
        assertThat(successCode, is(equalTo(HttpStatus.SC_OK)));
    }

    @Test
    void addRoleHandlerThrowsDataSyncExceptionWhenDatabaseServiceCannotFetchSavedRole()
        throws InvalidEntryInternalException {

        RoleDto inputRole = sampleRole();
        AddRoleHandler addRoleHandler = addRoleHandlerDoesNotFindRoleAfterAddingIt();
        var response = addRoleHandler.handleRequest(createRequest(inputRole), context);

        assertThat(response.getBody(), containsString(AddRoleHandler.ERROR_FETCHING_SAVED_ROLE));
    }

    @Test
    void errorMessageIsLoggedWhenAddRoleHandlerThrowsDatasyncException() {
        TestAppender testingAppender = LogUtils.getTestingAppenderForRootLogger();
        var addRoleHandler = addRoleHandlerDoesNotFindRoleAfterAddingIt();
        var inputRequest = createRequest(sampleRole);
        addRoleHandler.handleRequest(inputRequest, context);

        assertThat(testingAppender.getMessages(), containsString(AddRoleHandler.ERROR_FETCHING_SAVED_ROLE));
    }

    private RoleDto extractResponseBody(APIGatewayProxyResponseEvent response) throws IOException {
        var responseBody = response.getBody();
        return JSON.std.beanFrom(RoleDto.class, responseBody);
    }

    private APIGatewayProxyRequestEvent createRequest(RoleDto inputRole) {
        var body = attempt(() -> JSON.std.asString(inputRole)).orElseThrow();
        return createRequest(body);
    }

    private APIGatewayProxyRequestEvent createRequest(String body) {
        return new APIGatewayProxyRequestEvent()
            .withBody(body);
    }

    private RoleDto sampleRole() throws InvalidEntryInternalException {
        return RoleDto.newBuilder().withRoleName(SOME_ROLE_NAME).build();
    }

    private AddRoleHandler addRoleHandlerDoesNotFindRoleAfterAddingIt() {
        IdentityService databaseNotFoundingRoles = databaseServiceAddingButNotGettingARole();

        return new AddRoleHandler(databaseNotFoundingRoles);
    }

    private IdentityServiceImpl databaseServiceAddingButNotGettingARole() {
        return new IdentityServiceImpl(localDynamo) {

            @Override
            public RoleDto getRole(RoleDto queryObject) throws NotFoundException {
                throw new NotFoundException(RoleService.ROLE_NOT_FOUND_MESSAGE);
            }
        };
    }

    private IdentityServiceImpl databaseServiceWithSyncDelay() {
        return new IdentityServiceImpl(localDynamo) {
            private int counter = 0;

            @Override
            public RoleDto getRole(RoleDto queryObject) throws InvalidEntryInternalException, NotFoundException {
                if (counter == 0) {
                    counter++;
                    throw new NotFoundException(RoleService.ROLE_NOT_FOUND_MESSAGE);
                }
                return super.getRole(queryObject);
            }
        };
    }

    private APIGatewayProxyResponseEvent sendRequest(RoleDto role) {
        var input = createRequest(role);
        return addRoleHandler.handleRequest(input, context);
    }
}