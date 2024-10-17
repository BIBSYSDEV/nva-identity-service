package no.unit.nva.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;


class GetCurrentTermsConditionsHandlerTest  extends HandlerTest {
    private FakeContext context;
    private ByteArrayOutputStream outputStream;
    private GetCurrentTermsConditionsHandler handler;
    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;


    @BeforeEach
    public void setup() {
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
        handler = new GetCurrentTermsConditionsHandler();
    }

    @Test
    public void shouldReturnTheClientWhenItExists() throws IOException {

        var gatewayResponse = sendRequest(getInputStream(), TermsConditionsResponse.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream getInputStream() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
            .withRequestContext(getRequestContext())
            .build();
    }

    private ObjectNode getRequestContext() {
        return objectMapperWithEmpty.convertValue(
            Map.of("path", "/terms-and-conditions/current", "domainName", "SAMPLE_DOMAIN_NAME"), ObjectNode.class);
    }
}