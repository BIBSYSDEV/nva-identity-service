package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.get.GetCustomerHandler.IDENTIFIER;
import static no.unit.nva.customer.get.GetCustomerHandler.IDENTIFIER_IS_NOT_A_VALID_UUID;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomAllowFileUploadForTypes;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomDoiAgent;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetCustomerHandlerTest {

    public static final String MALFORMED_IDENTIFIER = "for-testing";
    public static final MediaType UNSUPPORTED_MEDIA_TYPE = MediaType.BZIP2;

    private CustomerService customerServiceMock;
    private GetCustomerHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        customerServiceMock = mock(CustomerService.class);
        handler = new GetCustomerHandler(customerServiceMock);

        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void requestToHandlerWithJsonLdAcceptHeaderReturnsJsonLdMediaType() throws IOException, NotFoundException {
        UUID identifier = UUID.randomUUID();
        prepareServiceWithCustomer(identifier);
        var supportedHeaders = new RequestHeaders(MediaTypes.APPLICATION_JSON_LD);
        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                .withHeaders(supportedHeaders.getRequestHeaders())
                .withAccessRights(randomUri(), MANAGE_CUSTOMERS)
                .withPathParameters(pathParameters)
                .build();

        var response = sendRequest(input, CustomerDto.class);

        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE),
                is(MediaTypes.APPLICATION_JSON_LD.toString()));
    }

    @Test
    void requestToHandlerReturnsCustomer() throws NotFoundException, IOException, BadRequestException {
        var identifier = UUID.randomUUID();
        var customerDto = prepareServiceWithCustomer(identifier);
        var inputStream = createGetCustomerRequest(customerDto);

        var response = sendRequest(inputStream, CustomerDto.class);

        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
        CustomerDto actualCustomerDto = CustomerDto.fromJson(response.getBody());
        assertThat(actualCustomerDto.getId(), notNullValue());
        assertThat(actualCustomerDto.getContext(), notNullValue());
        assertThat(actualCustomerDto.getDoiAgent(), notNullValue());
        assertThat(actualCustomerDto, is(customerDto));
    }

    @Test
    void requestToHandlerWithMalformedIdentifierReturnsBadRequest() throws IOException {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, MALFORMED_IDENTIFIER);
        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                .withHeaders(getRequestHeaders())
                .withAccessRights(randomUri(), MANAGE_CUSTOMERS)
                .withPathParameters(pathParameters)
                .build();

        var response = sendRequest(input, Problem.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_BAD_REQUEST));
        assertThat(response.getBody(), containsString(IDENTIFIER_IS_NOT_A_VALID_UUID + MALFORMED_IDENTIFIER));
    }

    @Test
    void requestToHandlerWithUnsupportedAcceptHeaderReturnsUnsupportedMediaType() throws IOException,
            NotFoundException {
        UUID identifier = UUID.randomUUID();
        prepareServiceWithCustomer(identifier);
        RequestHeaders unsupportedRequestHeaders = new RequestHeaders(UNSUPPORTED_MEDIA_TYPE);
        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                .withAccessRights(randomUri(), MANAGE_CUSTOMERS)
                .withHeaders(unsupportedRequestHeaders.getRequestHeaders())
                .withPathParameters(pathParameters)
                .build();

        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_UNSUPPORTED_TYPE));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream input, Class<T> responseType) throws IOException {
        handler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream createGetCustomerRequest(CustomerDto customerDto)
            throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, customerDto.getIdentifier().toString());
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper)
                .withBody(customerDto)
                .withAccessRights(randomUri(), MANAGE_CUSTOMERS)
                .withHeaders(getRequestHeaders())
                .withPathParameters(pathParameters)
                .build();
    }

    private CustomerDto prepareServiceWithCustomer(UUID identifier) throws NotFoundException {
        CustomerDao customerDb = new CustomerDao.Builder()
                .withIdentifier(identifier)
                .withCustomerOf(randomElement(ApplicationDomain.values()).getUri())
                .withDoiAgent(randomDoiAgent(randomString()))
                .withAllowFileUploadForTypes(randomAllowFileUploadForTypes())
                .build();
        CustomerDto customerDto = customerDb.toCustomerDto();
        when(customerServiceMock.getCustomer(identifier)).thenReturn(customerDto);
        return customerDto;
    }

    public static class RequestHeaders {

        private final MediaType mediaType;

        public RequestHeaders(MediaType mediaType) {
            this.mediaType = mediaType;
        }

        public Map<String, String> getRequestHeaders() {
            return Map.of(
                    HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
                    HttpHeaders.ACCEPT, mediaType.toString()
            );
        }
    }
}
