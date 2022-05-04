package no.unit.nva.handlers;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.RoleService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class AddRoleHandlerTest extends HandlerTest {

    public static final String SOME_ROLE_NAME = "someRoleName";
    private RoleDto sampleRole;
    private AddRoleHandler addRoleHandler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    /**
     * init.
     *
     * @throws InvalidEntryInternalException when an invalid role is created
     */
    @BeforeEach
    public void init() throws InvalidEntryInternalException {
        context = new FakeContext();
        IdentityService service = new IdentityServiceImpl(initializeTestDatabase());
        addRoleHandler = new AddRoleHandler(service);
        sampleRole = sampleRole();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void handleRequestReturnsBadRequestWhenRequestBodyIsEmpty() throws IOException {
        var response = sendRequest(null);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    void handleRequestReturnsBadRequestWhenRequestBodyIsAnEmptyObject() throws IOException {
        var response = sendRequest(new RoleDto());
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    void handlerRequestReturnsOkWheRequestBodyIsValid() throws InvalidEntryInternalException, IOException {
        var response = sendRequest(sampleRole());
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_OK)));
    }

    @Test
    void shouldReturnBadRequestWheRequestBodyIsInValid() throws InvalidEntryInternalException, IOException {
        var request = new HandlerRequestBuilder<String>(JsonUtils.dtoObjectMapper)
            .withBody(randomString())
            .build();
        addRoleHandler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    void handlerRequestReturnsTheGeneratedObjectWhenInputIsValid()
        throws InvalidEntryInternalException, IOException {
        var actualRole = sampleRole();
        var response = sendRequest(actualRole);
        RoleDto savedRole = response.getBodyObject(RoleDto.class);
        assertThat(savedRole, is(equalTo(actualRole)));
    }

    @Test
    void handlerRequestReturnsTheGeneratedObjectAfterWaitingForSyncingToComplete()
        throws InvalidEntryInternalException, IOException {
        var actualRole = sampleRole();
        var service = databaseServiceWithSyncDelay();
        addRoleHandler = new AddRoleHandler(service);
        var response = sendRequest(actualRole);
        var savedRole = response.getBodyObject(RoleDto.class);
        assertThat(savedRole, is(equalTo(actualRole)));
    }

    @Test
    void handleRequestReturnsInternalServerErrorWhenDatabaseFailsToSaveTheData()
        throws InvalidEntryInternalException, IOException {
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
        throws InvalidEntryInternalException, IOException {

        RoleDto inputRole = sampleRole();
        AddRoleHandler addRoleHandler = addRoleHandlerDoesNotFindRoleAfterAddingIt();
        var request = createRequest(inputRole).build();
        addRoleHandler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getBody(), containsString(AddRoleHandler.ERROR_FETCHING_SAVED_ROLE));
    }

    @Test
    void errorMessageIsLoggedWhenAddRoleHandlerThrowsDatasyncException() throws IOException {
        TestAppender testingAppender = LogUtils.getTestingAppenderForRootLogger();
        var addRoleHandler = addRoleHandlerDoesNotFindRoleAfterAddingIt();
        var inputRequest = createRequest(sampleRole).build();
        addRoleHandler.handleRequest(inputRequest, outputStream, context);

        assertThat(testingAppender.getMessages(), containsString(AddRoleHandler.ERROR_FETCHING_SAVED_ROLE));
    }

    private HandlerRequestBuilder<RoleDto> createRequest(RoleDto body) throws JsonProcessingException {
        return new HandlerRequestBuilder<RoleDto>(JsonUtils.dtoObjectMapper).withBody(body);
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

    private GatewayResponse<RoleDto> sendRequest(RoleDto role) throws IOException {
        var input = createRequest(role).build();
        addRoleHandler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, RoleDto.class);
    }
}