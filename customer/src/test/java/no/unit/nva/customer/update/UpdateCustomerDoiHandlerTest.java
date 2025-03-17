package no.unit.nva.customer.update;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.customer.exception.InputException;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDto;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.update.UpdateCustomerHandler.IDENTIFIER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_NVI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateCustomerDoiHandlerTest {

    private static final Context CONTEXT = new FakeContext();

    private CustomerDto existingCustomer;
    private DoiAgentDto existingDoiAgent;
    private ByteArrayOutputStream outputStream;
    private CustomerService customerServiceMock;
    private UpdateCustomerDoiHandler handler;

    @BeforeEach
    public void beforeEach() throws NotFoundException, InputException {
        this.outputStream = new ByteArrayOutputStream();
        customerServiceMock = mock(CustomerService.class);
        var secretsReaderMock = mock(SecretsReader.class);

        handler = new UpdateCustomerDoiHandler(customerServiceMock, mock(SecretsWriter.class), secretsReaderMock);
        existingCustomer = CustomerDataGenerator.createSampleActiveCustomerDao().toCustomerDto();
        existingDoiAgent = existingCustomer.getDoiAgent().addPassword(randomString());

        var doiAgent = createSampleCustomerDto().getDoiAgent().addPassword(randomString());
        var secretDaoArray = createSampleSecretDaos(doiAgent, existingDoiAgent, existingDoiAgent);

        when(customerServiceMock.getCustomer(any(UUID.class))
        ).thenReturn(existingCustomer);

        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))
        ).thenReturn(existingCustomer);

        when(secretsReaderMock.fetchSecret(any(), any())
        ).thenReturn(secretDaoArray);
    }

    private String createSampleSecretDaos(DoiAgentDto... args) {
        return Arrays.stream(args)
            .map(SecretManagerDoiAgentDao::new)
            .map(SecretManagerDoiAgentDao::toString)
            .collect(Collectors.joining(",", "[", "]"));
    }

    /**
     * Task.
     * <a href="https://unit.atlassian.net/browse/NP-27814">NP-27814</a>
     */
    @Test
    void shouldUpdateExistingSecretWhenIdentifierFoundInSecrets()
        throws ApiGatewayException, IOException {

        var secretPassword = randomString();

        var response = sendRequest(existingCustomer.getIdentifier(),
            doiAgentToJson(existingDoiAgent, secretPassword),
            String.class);
        var doiAgentResponse = DoiAgentDto.fromJson(response.getBody());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(doiAgentResponse.getPassword(), is(equalTo(secretPassword)));
    }

    private <T> GatewayResponse<T> sendRequest(UUID identifier, T body, Class<T> responseType) throws IOException {
        var request = createRequest(identifier, body);
        return sendRequest(request, responseType);
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, CONTEXT);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private <T> InputStream createRequest(UUID identifier, T body)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<T>(dtoObjectMapper)
            .withHeaders(getRequestHeaders())
            .withPathParameters(Map.of(IDENTIFIER, identifier.toString()))
            .withBody(body)
            .withCurrentCustomer(existingCustomer.getId())
            .withAccessRights(existingCustomer.getId(), MANAGE_CUSTOMERS)
            .build();
    }

    private String doiAgentToJson(DoiAgentDto doiAgent, String secretPassword) {
        return new DoiAgentDto(doiAgent)
            .addId(doiAgent.getId())
            .addPassword(secretPassword).toString();
    }

    /**
     * Task.
     * <a href="https://unit.atlassian.net/browse/NP-27812">NP-27812</a>
     * This also tests that secret is persisted with the identifier in the request.
     */
    @Test
    void shouldInsertSecretWhenIdentifierNotFoundInSecrets()
        throws ApiGatewayException, IOException {

        var expectedDoiAgent = createSampleCustomerDto().getDoiAgent().addPassword(randomString());
        var identifier = UUID.randomUUID();

        var response = sendRequest(identifier, expectedDoiAgent.toString(), String.class);
        var doiAgentResponse = DoiAgentDto.fromJson(response.getBody());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(expectedDoiAgent.getPassword(), is(equalTo(doiAgentResponse.getPassword())));
        assertThat(expectedDoiAgent.getId(), is(not(equalTo(doiAgentResponse.getId()))));
    }

    @Test
    void shouldReturnSecretWithEmptyUserNameWhenPersistingEmptyUserName()
        throws ApiGatewayException, IOException {


        existingDoiAgent.setUsername(null);
        var response = sendRequest(existingCustomer.getIdentifier(),
            existingDoiAgent.toString(),
            String.class);
        var doiAgentResponse = DoiAgentDto.fromJson(response.getBody());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(existingDoiAgent, is(equalTo(doiAgentResponse)));
    }

    @Test
    void shouldReturnExistingValuesWhenPersistingEmptyOrMissingFields()
        throws ApiGatewayException, IOException {

        existingDoiAgent.setUrl(null);
        existingDoiAgent.setPrefix(null);
        existingDoiAgent.setPassword(null);
        var response = sendRequest(existingCustomer.getIdentifier(),
            existingDoiAgent.toString(),
            String.class);
        var doiAgentResponse = DoiAgentDto.fromJson(response.getBody());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertNotNull(doiAgentResponse.getPassword());
        assertNotNull(doiAgentResponse.getPrefix());
        assertNotNull(doiAgentResponse.getUrl());
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

    private <T> InputStream createRequestWithInvalidUuid(T body)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<T>(dtoObjectMapper)
            .withHeaders(getRequestHeaders())
            .withPathParameters(Map.of(IDENTIFIER, randomString()))
            .withBody(body)
            .withCurrentCustomer(existingCustomer.getId())
            .withAccessRights(existingCustomer.getId(), MANAGE_CUSTOMERS)
            .build();
    }

    @Test
    void failUpdateRequestReturnsForbiddenRequestWhenARequestWithInvalidAccessRights()
        throws ApiGatewayException, IOException {
        when(
            customerServiceMock.getCustomer(any(UUID.class))
        ).thenThrow(NotFoundException.class);

        var response = sendRequest(createRequestWithInvalidAccessRights(null), Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    private <T> InputStream createRequestWithInvalidAccessRights(T body)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<T>(dtoObjectMapper)
            .withHeaders(getRequestHeaders())
            .withPathParameters(Map.of(IDENTIFIER, randomString()))
            .withBody(body)
            .withCurrentCustomer(existingCustomer.getId())
            .withAccessRights(existingCustomer.getId(), MANAGE_NVI)
            .build();
    }
}