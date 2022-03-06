package no.unit.nva.handlers;

import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.database.DatabaseAccessor;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.interfaces.WithEnvironment;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import nva.commons.apigatewayv2.exceptions.ApiGatewayException;
import nva.commons.apigatewayv2.exceptions.ConflictException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetRoleHandlerTest extends DatabaseAccessor implements WithEnvironment {

    public static final String THE_ROLE = "theRole";
    public static final String TYPE_ATTRIBUTE = "type";
    private IdentityServiceImpl databaseService;
    private GetRoleHandler getRoleHandler;
    private Context context;

    /**
     * init.
     */
    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        getRoleHandler = new GetRoleHandler(databaseService);
        context = new FakeContext();
    }

    @Test
    void statusCodeReturnsOkWhenRequestIsSuccessful() {
        Integer successCode = getRoleHandler.getSuccessStatusCode(null, null);
        assertThat(successCode, is(equalTo(HttpStatus.SC_OK)));
    }

    @DisplayName("handleRequest returns Role object with type \"Role\"")
    @Test
    void handleRequestReturnsRoleObjectWithTypeRole()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, IOException {

        addSampleRoleToDatabase();

        var request = queryWithRoleName();
        var response = getRoleHandler.handleRequest(request, context);

        var responseBody = objectMapper.mapFrom(response.getBody());
        assertThat(responseBody.get(TYPE_ATTRIBUTE), is(not(nullValue())));
        String type = responseBody.get(TYPE_ATTRIBUTE).toString();
        assertThat(type, is(equalTo(RoleDto.TYPE)));
    }

    @Test
    void shouldReturnRoleDtoWhenARoleWithTheInputRoleNameExists()
        throws ApiGatewayException, IOException {
        addSampleRoleToDatabase();
        var requestInfo = queryWithRoleName();
        var response = getRoleHandler.handleRequest(requestInfo, context);
        var savedRole = RoleDto.fromJson(response.getBody());
        assertThat(savedRole.getRoleName(), is(equalTo(THE_ROLE)));
    }

    @Test
    void shouldReturnNotFoundWhenThereIsNoRoleInTheDatabaseWithTheSpecifiedRoleName() {
        var requestInfo = queryWithRoleName();
        var response = getRoleHandler.handleRequest(requestInfo, context);
        assertThat(response.getBody(), containsString(THE_ROLE));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnBadRequestWheRequestBodyIsInValid() throws InvalidEntryInternalException {
        var request = new APIGatewayProxyRequestEvent().withBody(randomString());
        var response = getRoleHandler.handleRequest(request,context);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }


    private APIGatewayProxyRequestEvent queryWithRoleName() {
        return new APIGatewayProxyRequestEvent()
            .withPathParameters(Map.of(GetRoleHandler.ROLE_PATH_PARAMETER, GetRoleHandlerTest.THE_ROLE));
    }

    private void addSampleRoleToDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        RoleDto existingRole = RoleDto.newBuilder().withRoleName(GetRoleHandlerTest.THE_ROLE).build();
        databaseService.addRole(existingRole);
    }
}