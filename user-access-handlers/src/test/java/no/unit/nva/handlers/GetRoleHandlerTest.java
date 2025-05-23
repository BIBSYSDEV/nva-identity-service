package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.database.interfaces.WithEnvironment;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.Environment;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;

import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;

class GetRoleHandlerTest extends LocalIdentityService implements WithEnvironment {

    public static final String THE_ROLE = "theRole";
    public static final String TYPE_ATTRIBUTE = "type";
    private IdentityServiceImpl databaseService;
    private GetRoleHandler getRoleHandler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    /**
     * init.
     */
    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        getRoleHandler = new GetRoleHandler(databaseService, new Environment());
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
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

        var roleName = randomRoleName();
        addSampleRoleToDatabase(roleName);
        var request = queryWithRoleName(roleName);
        var response = sendRequest(request, RoleDto.class);

        var responseBody = JsonConfig.mapFrom(response.getBody());
        assertThat(responseBody.get(TYPE_ATTRIBUTE), is(not(nullValue())));
        String type = responseBody.get(TYPE_ATTRIBUTE).toString();
        assertThat(type, is(equalTo(RoleDto.TYPE)));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        getRoleHandler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream queryWithRoleName(RoleName roleName) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withPathParameters(Map.of(GetRoleHandler.ROLE_PATH_PARAMETER, roleName.getValue()))
            .build();
    }

    private void addSampleRoleToDatabase(RoleName roleName)
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        RoleDto existingRole = RoleDto.newBuilder().withRoleName(roleName).build();
        databaseService.addRole(existingRole);
    }

    @Test
    void shouldReturnRoleDtoWhenARoleWithTheInputRoleNameExists()
        throws ApiGatewayException, IOException {
        var roleName = randomRoleName();
        addSampleRoleToDatabase(roleName);
        var request = queryWithRoleName(roleName);
        var response = sendRequest(request, RoleDto.class);
        var savedRole = RoleDto.fromJson(response.getBody());
        assertThat(savedRole.getRoleName(), is(equalTo(roleName)));
    }

    @Test
    void shouldReturnNotFoundWhenThereIsNoRoleInTheDatabaseWithTheSpecifiedRoleName() throws IOException {
        var roleName = randomRoleName();
        var requestInfo = queryWithRoleName(roleName);
        var response = sendRequest(requestInfo, Problem.class);
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(roleName.getValue()));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnBadRequestWheRequestBodyIsInValid() throws InvalidEntryInternalException, IOException {
        var request = new HandlerRequestBuilder<String>(JsonUtils.dtoObjectMapper)
            .withBody(randomString())
            .build();
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }
}