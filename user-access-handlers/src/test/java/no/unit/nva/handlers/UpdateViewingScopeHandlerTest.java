package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.MediaType;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.ViewingScope;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.handlers.HandlerAccessingUser.USERNAME_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class UpdateViewingScopeHandlerTest extends HandlerTest {

    private static final Context CONTEXT = new FakeContext();
    private UpdateViewingScopeHandler handler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        createDatabaseServiceUsingLocalStorage();
        handler = new UpdateViewingScopeHandler(databaseService);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldUpdateAccessRightsWhenInputIsValidRequest()
        throws NotFoundException, ConflictException, IOException {
        UserDto sampleUser = addSampleUserToDb();
        var expectedViewingScope = randomViewingScope();
        var input = createUpdateViewingScopeRequest(sampleUser, expectedViewingScope);

        handler.handleRequest(input, outputStream, CONTEXT);
        var queryObject = UserDto.newBuilder().withUsername(sampleUser.getUsername()).build();
        var actualViewingScope = databaseService.getUser(queryObject).getViewingScope();
        assertThat(actualViewingScope, is(equalTo(expectedViewingScope)));
    }

    private UserDto addSampleUserToDb() throws ConflictException {
        var sampleUser = createSampleUserAndInsertUserRoles();
        databaseService.addUser(sampleUser);
        return sampleUser;
    }

    private InputStream createUpdateViewingScopeRequest(UserDto sampleUser,
                                                        ViewingScope expectedViewingScope)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<ViewingScope>(dtoObjectMapper)
            .withBody(expectedViewingScope)
            .withPathParameters(Map.of(USERNAME_PATH_PARAMETER, sampleUser.getUsername()))
            .build();
    }

    @Test
    void shouldReturnAcceptedWhenInputIsValidAndUpdateHasBeenSubmittedToEventuallyConsistentDb()
        throws ConflictException, IOException {
        var sampleUser = addSampleUserToDb();
        var request = createUpdateViewingScopeRequest(sampleUser, randomViewingScope());
        var response = sendRequest(request, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }

    private <I> GatewayResponse<I> sendRequest(InputStream request, Class<I> responseType) throws IOException {
        handler.handleRequest(request, outputStream, CONTEXT);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    @Test
    void shouldReturnNotFoundWhenUsernameDoesNotExist() throws IOException {
        var sampleUser = createSampleUserAndInsertUserRoles();
        var request = createUpdateViewingScopeRequest(sampleUser, randomViewingScope());
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsNotValidViewingScope()
        throws ConflictException, IOException {
        var sampleUser = addSampleUserToDb();
        var request = createInvalidUpdateViewingScopeRequest(sampleUser);
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    private InputStream createInvalidUpdateViewingScopeRequest(UserDto objectThatIsNotViewingScope)
        throws JsonProcessingException {
        var jsonMap = attempt(() -> JsonConfig.writeValueAsString(objectThatIsNotViewingScope))
            .map(JsonConfig::mapFrom)
            .orElseThrow();
        jsonMap.remove("type");
        return new HandlerRequestBuilder<Map<String,?>>(dtoObjectMapper)
            .withBody(jsonMap)
            .withPathParameters(Map.of(USERNAME_PATH_PARAMETER, objectThatIsNotViewingScope.getUsername()))
            .build();
    }

    @Test
    void shouldContainContentTypeHeaderWithValueJson()
        throws ConflictException, IOException {
        var sampleUser = addSampleUserToDb();
        var request = createUpdateViewingScopeRequest(sampleUser, randomViewingScope());
        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
        assertThat(response.getHeaders(), hasEntry(CONTENT_TYPE, MediaType.JSON_UTF_8.toString()));
    }

    @Test
    void shouldReturnBadRequestWheRequestBodyIsInValid() throws InvalidEntryInternalException, IOException {
        var request = new HandlerRequestBuilder<String>(dtoObjectMapper)
            .withBody(randomString())
            .build();
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }
}