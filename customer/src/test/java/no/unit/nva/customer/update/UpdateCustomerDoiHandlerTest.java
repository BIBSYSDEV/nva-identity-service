package no.unit.nva.customer.update;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.update.UpdateCustomerHandler.IDENTIFIER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.ADMINISTRATE_APPLICATION;
import static nva.commons.apigateway.AccessRight.USER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.model.SecretManagerDoiAgentDao;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.secrets.SecretsReader;
import nva.commons.secrets.SecretsWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class UpdateCustomerDoiHandlerTest {

    private static final Context CONTEXT = new FakeContext();

    private CustomerDto existingCustomer;
    private ByteArrayOutputStream outputStream;
    private CustomerService customerServiceMock;
    private SecretsReader secretsReaderMock;
    private UpdateCustomerDoiHandler handler;

    @BeforeEach
    public void beforeEach() {
        this.outputStream = new ByteArrayOutputStream();
        customerServiceMock = mock(CustomerService.class);
        var secretWriterMock = mock(SecretsWriter.class);
        secretsReaderMock = mock(SecretsReader.class);
        handler = new UpdateCustomerDoiHandler(customerServiceMock, secretWriterMock, secretsReaderMock);
        existingCustomer = CustomerDataGenerator.createSampleCustomerDao().toCustomerDto();
    }

    @Test
    void handleUpdateRequestOK()
        throws ApiGatewayException, IOException {

        var secretPassword = randomString();
        var customer2 = CustomerDataGenerator.createSampleCustomerDao().toCustomerDto();

        var doiAgent = existingCustomer.getDoiAgent().addPassword(randomString());
        var doiAgent2 = customer2.getDoiAgent().addPassword(randomString());

        var secretDaoArray = "[" + new SecretManagerDoiAgentDao(doiAgent) + ", "
                             + new SecretManagerDoiAgentDao(doiAgent2) + "]";

        when(customerServiceMock.getCustomer(any(UUID.class))
        ).thenReturn(existingCustomer);

        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))
        ).thenReturn(existingCustomer);

        when(secretsReaderMock.fetchSecret(any(), any())
        ).thenReturn(secretDaoArray);

        var response = sendRequest(getExistingCustomerIdentifier(),
                                   new DoiAgentDto()
                                       .addId(doiAgent.getId())
                                       .addPassword(secretPassword).toString(),
                                   String.class);
        var doiAgentResponse =  DoiAgentDto.fromJson(response.getBody());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(doiAgentResponse.getPassword(), is(equalTo(secretPassword)));
    }

    @Test
    void failUpdateRequestReturnsBadRequestWhenARequestWithAInvalidIdentifier()
        throws ApiGatewayException, IOException {
        when(
            customerServiceMock.getCustomer(any(UUID.class))
        ).thenThrow(NotFoundException.class);

        var response = sendRequest(createRequestWithInvalidUuid(null), Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    void handleinvalidUserAccess()
        throws ApiGatewayException, IOException {
        when(
            customerServiceMock.getCustomer(any(UUID.class))
        ).thenThrow(NotFoundException.class);

        var response = sendRequest(createRequestWithInvalidAccessRights(null), Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, CONTEXT);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private <T> GatewayResponse<T> sendRequest(UUID identifier, T body, Class<T> responseType) throws IOException {
        var request = createRequest(identifier, body);
        return sendRequest(request, responseType);
    }

    private UUID getExistingCustomerIdentifier() {
        return existingCustomer.getIdentifier();
    }

    private <T> InputStream createRequest(UUID identifier, T body)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<T>(dtoObjectMapper)
                   .withHeaders(getRequestHeaders())
                   .withPathParameters(Map.of(IDENTIFIER, identifier.toString()))
                   .withBody(body)
                   .withCurrentCustomer(existingCustomer.getId())
                   .withAccessRights(existingCustomer.getId(), ADMINISTRATE_APPLICATION.toString())
                   .build();
    }

    private <T> InputStream createRequestWithInvalidUuid(T body)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<T>(dtoObjectMapper)
                   .withHeaders(getRequestHeaders())
                   .withPathParameters(Map.of(IDENTIFIER, randomString()))
                   .withBody(body)
                   .withCurrentCustomer(existingCustomer.getId())
                   .withAccessRights(existingCustomer.getId(), ADMINISTRATE_APPLICATION.toString())
                   .build();
    }

    private <T> InputStream createRequestWithInvalidAccessRights(T body)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<T>(dtoObjectMapper)
                   .withHeaders(getRequestHeaders())
                   .withPathParameters(Map.of(IDENTIFIER, randomString()))
                   .withBody(body)
                   .withCurrentCustomer(existingCustomer.getId())
                   .withAccessRights(existingCustomer.getId(), USER.toString())
                   .build();
    }
}