package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.UserDto;
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

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

class GetUserHandlerTest extends HandlerTest {

    private Context context;
    private GetUserHandler getUserHandler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        getUserHandler = new GetUserHandler(databaseService, new Environment());
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @DisplayName("handleRequest returns User object with type \"User\"")
    @Test
    void handleRequestReturnsUserObjectWithTypeRole()
        throws InvalidEntryInternalException, InvalidInputException, IOException, ConflictException {
        insertSampleUserToDatabase();

        var request = createRequest(DEFAULT_USERNAME);
        var response = sendRequest(request, Map.class);
        var bodyObject = JsonConfig.mapFrom(response.getBody());

        assertThat(bodyObject.get(TYPE_ATTRIBUTE), is(not(nullValue())));
        String type = bodyObject.get(TYPE_ATTRIBUTE).toString();
        assertThat(type, is(equalTo(UserDto.TYPE)));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseBodyType) throws IOException {
        getUserHandler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseBodyType);
    }

    private InputStream createRequest(String username) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
            .withPathParameters(Map.of(GetUserHandler.USERNAME_PATH_PARAMETER, username))
            .build();
    }

    @Test
    void getSuccessStatusCodeReturnsOK() {
        Integer actual = getUserHandler.getSuccessStatusCode(null, null);
        assertThat(actual, is(equalTo(HttpStatus.SC_OK)));
    }

    @Test
    void shouldReturnUserDtoWhenPathParameterContainsTheUsernameOfExistingUser()
        throws ApiGatewayException, IOException {
        var request = createRequest(DEFAULT_USERNAME);
        UserDto expected = insertSampleUserToDatabase();
        var response = sendRequest(request, UserDto.class);
        var actual = response.getBodyObject(UserDto.class);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldReturnUserDtoWhenPathParameterContainsTheUsernameOfExistingUserEnc()
        throws ApiGatewayException, IOException {

        String encodedUserName = encodeString(DEFAULT_USERNAME);
        var request = createRequest(encodedUserName);
        UserDto expected = insertSampleUserToDatabase();
        var response = sendRequest(request, UserDto.class);
        var actual = UserDto.fromJson(response.getBody());
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldReturnNotFoundExceptionWhenPathParameterIsNonExistingUsername() throws IOException {

        var request = createRequest(DEFAULT_USERNAME);
        var response = sendRequest(request, UserDto.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnBadRequestWheRequestBodyIsInValid() throws InvalidEntryInternalException, IOException {
        var request = new HandlerRequestBuilder<String>(dtoObjectMapper).withBody(randomString()).build();
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }
}