package no.unit.nva.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.CreateExternalClientRequest;
import no.unit.nva.useraccessservice.model.GetExternalClientResponse;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class GetExternalClientUserinfoHandlerTest extends HandlerTest {

    private FakeContext context;
    private ByteArrayOutputStream outputStream;
    private GetExternalClientUserinfoHandler handler;

    @BeforeEach
    public void setup() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
        handler = new GetExternalClientUserinfoHandler(databaseService, new Environment());
    }

    @Test
    public void shouldReturnTheClientWithOnlyExternalToken() throws IOException {
        var client =
            ClientDto.newBuilder()
                .withClientId("someClientId")
                .withCristinOrgUri(RandomDataGenerator.randomUri())
                .withCustomer(RandomDataGenerator.randomUri())
                .withActingUser("someone@123")
                .build();

        insertClientToDatabase(client);
        var gatewayResponse = sendRequest(createRequestWithClientInToken("someClientId"),
            GetExternalClientResponse.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(gatewayResponse.getBody(), containsString("customerUri"));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream createRequestWithClientInToken(String clientId)
        throws JsonProcessingException {

        return new HandlerRequestBuilder<CreateExternalClientRequest>(dtoObjectMapper)
            .withClientId(clientId)
            .build();
    }
}