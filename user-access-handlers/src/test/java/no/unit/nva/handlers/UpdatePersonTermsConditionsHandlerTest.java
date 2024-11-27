package no.unit.nva.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.database.TermsAndConditionsService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class UpdatePersonTermsConditionsHandlerTest extends HandlerTest {
    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    private FakeContext context;
    private ByteArrayOutputStream outputStream;
    private UpdatePersonTermsConditionsHandler handler;
    private TermsAndConditionsService mockedService;
    private TermsConditionsResponse response;


    @BeforeEach
    public void setup() {
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
        mockedService = mock(TermsAndConditionsService.class);
        handler = new UpdatePersonTermsConditionsHandler(mockedService);
        response = TermsConditionsResponse.builder()
            .withTermsConditionsUri(randomUri())
            .build();
    }

    @Test
    public void shouldReturnTheClientWhenItExists() throws IOException, NotFoundException {


        when(mockedService.updateTermsAndConditions(any(), any(), any())).thenReturn(response);


        var gatewayResponse = sendRequest(getInputStream(), TermsConditionsResponse.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(gatewayResponse.getBodyObject(TermsConditionsResponse.class), is(equalTo(response)));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream getInputStream() throws JsonProcessingException {
        return new HandlerRequestBuilder<TermsConditionsResponse>(objectMapperWithEmpty)
            .withRequestContext(getRequestContext())
            .withPersonCristinId(randomUri())
            .withCurrentCustomer(randomUri())
            .withBody(response)
            .build();
    }

    private ObjectNode getRequestContext() {
        return objectMapperWithEmpty.convertValue(
            Map.of("path", "/terms-and-conditions/", "domainName", "SAMPLE_DOMAIN_NAME"), ObjectNode.class);
    }
}