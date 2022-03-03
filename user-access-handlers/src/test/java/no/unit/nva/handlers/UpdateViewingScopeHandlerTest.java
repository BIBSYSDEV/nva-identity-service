package no.unit.nva.handlers;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.handlers.HandlerAccessingUser.USERNAME_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.jr.ob.JSON;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import nva.commons.apigatewayv2.exceptions.ConflictException;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateViewingScopeHandlerTest extends HandlerTest {

    private static final Context CONTEXT = mock(Context.class);
    private UpdateViewingScopeHandler handler;


    @BeforeEach
    public void init() {
        createDatabaseServiceUsingLocalStorage();
        handler = new UpdateViewingScopeHandler(databaseService);
    }

    @Test
    void shouldUpdateAccessRightsWhenInputIsValidRequest()
        throws InvalidInputException, NotFoundException, ConflictException {
        UserDto sampleUser = addSampleUserToDb();
        var expectedViewingScope = randomViewingScope();
        var input = createUpdateViewingScopeRequest(sampleUser, expectedViewingScope);

        handler.handleRequest(input, CONTEXT);
        var queryObject = UserDto.newBuilder().withUsername(sampleUser.getUsername()).build();
        var actualViewingScope = databaseService.getUser(queryObject).getViewingScope();
        assertThat(actualViewingScope, is(equalTo(expectedViewingScope)));
    }

    @Test
    void shouldReturnAcceptedWhenInputIsValidAndUpdateHasBeenSubmittedToEventuallyConsistentDb()
        throws InvalidInputException, ConflictException {
        var sampleUser = addSampleUserToDb();
        var request = createUpdateViewingScopeRequest(sampleUser, randomViewingScope());
        var response = handler.handleRequest(request, CONTEXT);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }

    @Test
    void shouldReturnNotFoundWhenUsernameDoesNotExist() {
        var sampleUser = createSampleUserAndInsertUserRoles();
        var request = createUpdateViewingScopeRequest(sampleUser, randomViewingScope());
        var response = handler.handleRequest(request, CONTEXT);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsNotValidViewingScope()
        throws InvalidInputException, ConflictException {
        var sampleUser = addSampleUserToDb();
        var request = createInvalidUpdateViewingScopeRequest(sampleUser);
        var response = handler.handleRequest(request, CONTEXT);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    void shouldContainContentTypeHeaderWithValueJson()
        throws InvalidInputException, ConflictException {
        var sampleUser = addSampleUserToDb();
        var request = createUpdateViewingScopeRequest(sampleUser, randomViewingScope());
        var response = handler.handleRequest(request, CONTEXT);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
        assertThat(response.getHeaders(), hasEntry(CONTENT_TYPE, MediaType.JSON_UTF_8.toString()));
    }

    @Test
    void shouldReturnBadRequestWheRequestBodyIsInValid() throws InvalidEntryInternalException {
        var request = new APIGatewayProxyRequestEvent().withBody(randomString());
        var response = handler.handleRequest(request,CONTEXT);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    private UserDto addSampleUserToDb() throws ConflictException, InvalidInputException {
        var sampleUser = createSampleUserAndInsertUserRoles();
        databaseService.addUser(sampleUser);
        return sampleUser;
    }

    private APIGatewayProxyRequestEvent createUpdateViewingScopeRequest(UserDto sampleUser,
                                                                        ViewingScope expectedViewingScope) {
        String bodyString = attempt(() -> JSON.std.asString(expectedViewingScope)).orElseThrow();
        return new APIGatewayProxyRequestEvent()
            .withBody(bodyString)
            .withPathParameters(Map.of(USERNAME_PATH_PARAMETER, sampleUser.getUsername()));
    }

    private APIGatewayProxyRequestEvent createInvalidUpdateViewingScopeRequest(UserDto objectThatIsNotViewingScope) {
        var jsonMap = attempt(() -> JSON.std.asString(objectThatIsNotViewingScope))
            .map(JSON.std::mapFrom)
            .orElseThrow();
        jsonMap.remove("type");
        String body = attempt(()->JSON.std.asString(jsonMap)).orElseThrow();
        return new APIGatewayProxyRequestEvent().withBody(body)
            .withPathParameters(Map.of(USERNAME_PATH_PARAMETER, objectThatIsNotViewingScope.getUsername()));
    }
}