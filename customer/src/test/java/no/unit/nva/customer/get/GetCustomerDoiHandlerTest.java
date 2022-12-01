package no.unit.nva.customer.get;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class GetCustomerDoiHandlerTest  {

    private static final Context CONTEXT = new FakeContext();

    private DoiAgentDto mockResponse;
    private CustomerDto existingCustomer;
    private ByteArrayOutputStream outputStream;
    private CustomerService customerServiceMock;
    private GetCustomerDoiHandler handler;

    @BeforeEach
    public void beforeEach() {
        this.outputStream = new ByteArrayOutputStream();
        customerServiceMock = mock(CustomerService .class);
        handler = new GetCustomerDoiHandler(customerServiceMock);
        existingCustomer = CustomerDataGenerator.createSampleCustomerDao().toCustomerDto();
        mockResponse = existingCustomer.getDoiAgent().setSecret(randomString());
    }

    @Test
    void handleRequestReturnsOkWhenARequestWithAnExistingIdentifier() throws IOException, NotFoundException {

        when(customerServiceMock.getCustomerDoiAgentSecret(eq(getExistingCustomerIdentifier())))
            .thenReturn(mockResponse);

        var response = sendRequest(getExistingCustomerIdentifier(), DoiAgentDto.class);
        var doiAgentResponse = response.getBodyObject(DoiAgentDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(doiAgentResponse.getName(), is(equalTo(existingCustomer.getDoiAgent().getName())));
    }

    @Test
    void handleRequestReturnsNotFoundWhenARequestWithANonExistingIdentifier() throws IOException {
        var response = sendRequest(randomCustomerIdentifier(), Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)));
    }


    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, CONTEXT);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private <T> GatewayResponse<T> sendRequest(UUID identifier, Class<T> responseType) throws IOException {
        var request = createRequestWithMediaType(identifier, MediaTypes.APPLICATION_JSON_LD);
        return sendRequest(request, responseType);
    }

    private UUID getExistingCustomerIdentifier() {
        return existingCustomer.getIdentifier();
    }

    private InputStream createRequestWithMediaType(UUID identifier, MediaType acceptHeader)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withPathParameters(Map.of("identifier", identifier.toString()))
                   .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptHeader.toString()))
                   .build();
    }

    private UUID randomCustomerIdentifier() {
        return UUID.randomUUID();
    }
}