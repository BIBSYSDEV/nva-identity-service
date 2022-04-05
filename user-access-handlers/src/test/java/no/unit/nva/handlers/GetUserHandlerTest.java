package no.unit.nva.handlers;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigatewayv2.exceptions.ApiGatewayException;
import nva.commons.apigatewayv2.exceptions.ConflictException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetUserHandlerTest extends HandlerTest {

    private Context context;
    private GetUserHandler getUserHandler;

    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        getUserHandler = new GetUserHandler(databaseService);
        context = new FakeContext();
    }

    @DisplayName("handleRequest returns User object with type \"User\"")
    @Test
    void handleRequestReturnsUserObjectWithTypeRole()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, IOException {
        insertSampleUserToDatabase();

        var request = createRequest(DEFAULT_USERNAME);
        var response = getUserHandler.handleRequest(request, context);

        var bodyObject = JsonConfig.mapFrom(response.getBody());

        assertThat(bodyObject.get(TYPE_ATTRIBUTE), is(not(nullValue())));
        String type = bodyObject.get(TYPE_ATTRIBUTE).toString();
        assertThat(type, is(equalTo(UserDto.TYPE)));
    }

    @Test
    void getSuccessStatusCodeReturnsOK() {
        Integer actual = getUserHandler.getSuccessStatusCode(null, null);
        assertThat(actual, is(equalTo(HttpStatus.SC_OK)));
    }

    @Test
    void shouldReturnUserDtoWhenPathParameterContainsTheUsernameOfExistingUser()
        throws ApiGatewayException {
        var request = createRequest(DEFAULT_USERNAME);
        UserDto expected = insertSampleUserToDatabase();
        var response = getUserHandler.handleRequest(request, context);
        var actual = UserDto.fromJson(response.getBody());

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldReturnUserDtoWhenPathParameterContainsTheUsernameOfExistingUserEnc()
        throws ApiGatewayException {

        String encodedUserName = encodeString(DEFAULT_USERNAME);
        var request = createRequest(encodedUserName);
        UserDto expected = insertSampleUserToDatabase();
        var response = getUserHandler.handleRequest(request, context);
        var actual = UserDto.fromJson(response.getBody());
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldReturnNotFoundExceptionWhenPathParameterIsNonExistingUsername() {

        var request = createRequest(DEFAULT_USERNAME);
        var response = getUserHandler.handleRequest(request, context);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnBadRequestWheRequestBodyIsInValid() throws InvalidEntryInternalException {
        var request = new APIGatewayProxyRequestEvent().withBody(randomString());
        var response = getUserHandler.handleRequest(request, context);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    private APIGatewayProxyRequestEvent createRequest(String username) {
        return new APIGatewayProxyRequestEvent()
            .withPathParameters(Map.of(GetUserHandler.USERNAME_PATH_PARAMETER, username));
    }
}