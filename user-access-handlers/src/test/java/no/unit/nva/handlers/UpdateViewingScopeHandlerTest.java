package no.unit.nva.handlers;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.handlers.HandlerAccessingUser.USERNAME_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Set;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class UpdateViewingScopeHandlerTest extends HandlerTest {

    private static final Context CONTEXT = mock(Context.class);
    private UpdateViewingScopeHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void init() {
        createDatabaseServiceUsingLocalStorage();
        handler = new UpdateViewingScopeHandler(databaseService);
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldUpdateAccessRightsWhenInputIsValidRequest()
        throws IOException, InvalidInputException, NotFoundException, ConflictException, BadRequestException {
        UserDto sampleUser = addSampleUserToDb();
        var expectedViewingScope = randomViewingScope();
        var input = createUpdateViewingScopeRequest(sampleUser, expectedViewingScope);

        handler.handleRequest(input, output, CONTEXT);
        var queryObject = UserDto.newBuilder().withUsername(sampleUser.getUsername()).build();
        var actualViewingScope = databaseService.getUser(queryObject).getViewingScope();
        assertThat(actualViewingScope, is(equalTo(expectedViewingScope)));
    }

    @Test
    void shouldReturnAcceptedWhenInputIsValidAndUpdateHasBeenSubmittedToEventuallyConsistentDb()
        throws InvalidInputException, ConflictException, IOException, BadRequestException {
        var sampleUser = addSampleUserToDb();
        var request = createUpdateViewingScopeRequest(sampleUser, randomViewingScope());
        handler.handleRequest(request, output, CONTEXT);
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }

    @Test
    void shouldReturnNotFoundWhenUsernameDoesNotExist() throws IOException, BadRequestException {
        var sampleUser = createSampleUserWithExistingRoles();
        var request = createUpdateViewingScopeRequest(sampleUser, randomViewingScope());
        handler.handleRequest(request, output, CONTEXT);
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsNotValidViewingScope()
        throws IOException, InvalidInputException, ConflictException {
        var sampleUser = addSampleUserToDb();
        var request = createInvalidUpdateViewingScopeRequest(sampleUser);
        handler.handleRequest(request, output, CONTEXT);
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }


    @Test
    void shouldReturnProblemWhenRequestIsNotSuccessful()
        throws IOException, InvalidInputException, ConflictException {
        var sampleUser = addSampleUserToDb();
        var request = createInvalidUpdateViewingScopeRequest(sampleUser);
        handler.handleRequest(request, output, CONTEXT);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(output);
        Problem problem = response.getBodyObject(Problem.class);
        assertThat(response.getHeaders(), hasEntry(CONTENT_TYPE, MediaTypes.APPLICATION_PROBLEM_JSON.toString()));
        assertThat(problem,is(not(nullValue())));
    }

    @Test
    void shouldContainContentTypeHeaderWithValueJson()
        throws IOException, InvalidInputException, ConflictException, BadRequestException {
        var sampleUser = addSampleUserToDb();
        var request = createUpdateViewingScopeRequest(sampleUser,randomViewingScope());
        handler.handleRequest(request, output, CONTEXT);
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
        assertThat(response.getHeaders(), hasEntry(CONTENT_TYPE, MediaType.JSON_UTF_8.toString()));
    }

    private UserDto addSampleUserToDb() throws ConflictException, InvalidInputException {
        var sampleUser = createSampleUserWithExistingRoles();
        databaseService.addUser(sampleUser);
        return sampleUser;
    }

    private InputStream createUpdateViewingScopeRequest(UserDto sampleUser, ViewingScope expectedViewingScope)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<ViewingScope>(JsonUtils.dtoObjectMapper)
            .withBody(expectedViewingScope)
            .withPathParameters(Map.of(USERNAME_PATH_PARAMETER, sampleUser.getUsername()))
            .build();
    }

    private InputStream createInvalidUpdateViewingScopeRequest(UserDto objectThatIsNotViewingScope)
        throws JsonProcessingException {
        ObjectNode json = JsonUtils.dtoObjectMapper.convertValue(objectThatIsNotViewingScope, ObjectNode.class);
        json.remove("type");
        return new HandlerRequestBuilder<JsonNode>(JsonUtils.dtoObjectMapper)
            .withBody(json)
            .withPathParameters(Map.of(USERNAME_PATH_PARAMETER, objectThatIsNotViewingScope.getUsername()))
            .build();
    }

    private ViewingScope randomViewingScope() throws BadRequestException {
        return new ViewingScope(Set.of(randomUri()), Set.of(randomUri()));
    }
}