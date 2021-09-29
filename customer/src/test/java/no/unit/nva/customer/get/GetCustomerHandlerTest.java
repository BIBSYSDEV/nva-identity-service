package no.unit.nva.customer.get;

import static no.unit.nva.customer.get.GetCustomerHandler.IDENTIFIER;
import static no.unit.nva.customer.get.GetCustomerHandler.IDENTIFIER_IS_NOT_A_VALID_UUID;
import static no.unit.nva.customer.testing.TestHeaders.getErrorResponseHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getResponseHeaders;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_REQUEST;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

public class GetCustomerHandlerTest {

    public static final String WILDCARD = "*";
    public static final String SAMPLE_NAMESPACE = "http://example.org/customer";
    public static final String REQUEST_ID = "requestId";
    public static final String MALFORMED_IDENTIFIER = "for-testing";
    public static final MediaType UNSUPPORTED_MEDIA_TYPE = MediaType.BZIP2;

    private CustomerMapper customerMapper;
    private ObjectMapper objectMapper = ObjectMapperConfig.objectMapper;
    private CustomerService customerServiceMock;
    private Environment environmentMock;
    private GetCustomerHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        customerServiceMock = mock(CustomerService.class);
        environmentMock = mock(Environment.class);
        customerMapper = new CustomerMapper(SAMPLE_NAMESPACE);
        when(environmentMock.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        handler = new GetCustomerHandler(customerServiceMock, customerMapper, environmentMock);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    public void requestToHandlerReturnsCustomer() throws Exception {
        UUID identifier = UUID.randomUUID();
        CustomerDto customerDto = prepareServiceWithCustomer(identifier);

        InputStream inputStream = createGetCustomerRequest(customerDto);
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse actual = GatewayResponse.fromOutputStream(outputStream);

        GatewayResponse<CustomerDto> expected = new GatewayResponse<>(
            objectMapper.writeValueAsString(customerDto),
            getResponseHeaders(),
            HttpStatus.SC_OK
        );

        //TODO: assert responses properly, one response has explicit null values in serialization
        assertEquals(expected.getStatusCode(), actual.getStatusCode());
    }

    @Test
    public void requestToHandlerWithMalformedIdentifierReturnsBadRequest() throws Exception {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, MALFORMED_IDENTIFIER);
        InputStream inputStream = new HandlerRequestBuilder<CustomerDb>(objectMapper)
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse actual = GatewayResponse.fromOutputStream(outputStream);

        GatewayResponse<Problem> expected = new GatewayResponse<>(
            Problem.builder()
                .withStatus(BAD_REQUEST)
                .withTitle(BAD_REQUEST.getReasonPhrase())
                .withDetail(IDENTIFIER_IS_NOT_A_VALID_UUID + MALFORMED_IDENTIFIER)
                .with(REQUEST_ID, null)
                .build(),
            getErrorResponseHeaders(),
            SC_BAD_REQUEST
        );

        assertEquals(expected, actual);
    }

    @Test
    public void requestToHandlerWithUnsupportedAcceptHeaderReturnsUnsupportedMediaType() throws Exception {
        UUID identifier = UUID.randomUUID();
        prepareServiceWithCustomer(identifier);

        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        InputStream inputStream = new HandlerRequestBuilder<CustomerDb>(objectMapper)
            .withHeaders(getRequestHeadersWithUnsupportedMediaType())
            .withPathParameters(pathParameters)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse actual = GatewayResponse.fromOutputStream(outputStream);

        assertEquals(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, actual.getStatusCode());
    }

    @Test
    public void requestToHandlerWithJsonLdAcceptHeaderReturnsJsonLdMediaType() throws Exception {
        UUID identifier = UUID.randomUUID();
        prepareServiceWithCustomer(identifier);

        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        InputStream inputStream = new HandlerRequestBuilder<CustomerDb>(objectMapper)
            .withHeaders(getRequestHeadersWithMediaType(MediaTypes.APPLICATION_JSON_LD))
            .withPathParameters(pathParameters)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse actual = GatewayResponse.fromOutputStream(outputStream);

        assertEquals(HttpURLConnection.HTTP_OK, actual.getStatusCode());
        assertEquals(MediaTypes.APPLICATION_JSON_LD.toString(), actual.getHeaders().get(HttpHeaders.CONTENT_TYPE));
    }

    private static Map<String, String> getRequestHeadersWithUnsupportedMediaType() {
        return Map.of(
            HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
            HttpHeaders.ACCEPT, UNSUPPORTED_MEDIA_TYPE.toString()
        );
    }

    private static Map<String, String> getRequestHeadersWithMediaType(MediaType mediaType) {
        return Map.of(
            HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
            HttpHeaders.ACCEPT, mediaType.toString()
        );
    }

    private InputStream createGetCustomerRequest(CustomerDto customerDto) throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, customerDto.getIdentifier().toString());
        InputStream inputStream = new HandlerRequestBuilder<CustomerDto>(objectMapper)
            .withBody(customerDto)
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters)
            .build();
        return inputStream;
    }

    private CustomerDto prepareServiceWithCustomer(UUID identifier) throws ApiGatewayException {
        CustomerDb customerDb = new CustomerDb.Builder()
            .withIdentifier(identifier)
            .build();
        when(customerServiceMock.getCustomer(identifier)).thenReturn(customerDb);

        CustomerDto customerDto = customerMapper.toCustomerDto(customerDb);
        return customerDto;
    }
}
