package no.unit.nva.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.CreateExternalClientRequest;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfoConstants;
import nva.commons.apigateway.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.handlers.GetExternalClientHandler.CLIENT_ID_PATH_PARAMETER_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class GetExternalClientHandlerTest extends HandlerTest {

    private FakeContext context;
    private ByteArrayOutputStream outputStream;
    private GetExternalClientHandler handler;

    @BeforeEach
    public void setup() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
        handler = new GetExternalClientHandler(databaseService);
    }

    @Test
    public void shouldReturnNotFoundWhenTheClientDoesNotExist() throws IOException {
        var gatewayResponse = sendRequest(createBackendRequest("someClientId"), Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }

    @Test
    public void shouldReturnTheClientWhenItExists() throws IOException, ConflictException {
        var client =
                ClientDto.newBuilder()
                        .withClientId("someClientId")
                        .withCristinOrgUri(RandomDataGenerator.randomUri())
                        .withCustomer(RandomDataGenerator.randomUri())
                        .withActingUser("someone@123")
                        .build();

        insertClientToDatabase(client);
        var gatewayResponse = sendRequest(createBackendRequest("someClientId"), Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream createBackendRequest(String clientId)
            throws JsonProcessingException {
        var pathParams = Map.of(CLIENT_ID_PATH_PARAMETER_NAME, clientId);

        return new HandlerRequestBuilder<CreateExternalClientRequest>(dtoObjectMapper)
                .withScope(RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE)
                .withPathParameters(pathParams)
                .build();
    }
}