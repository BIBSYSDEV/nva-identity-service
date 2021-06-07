package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import static no.unit.nva.customer.get.GetCustomerHandler.IDENTIFIER;
import static no.unit.nva.customer.get.GetCustomerHandler.IDENTIFIER_IS_NOT_A_VALID_UUID;
import static no.unit.nva.customer.testing.TestHeaders.getErrorResponseHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getResponseHeaders;
import static nva.commons.handlers.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_REQUEST;

public class GetCustomerHandlerTest {

    public static final String WILDCARD = "*";
    public static final String SAMPLE_NAMESPACE = "http://example.org/customer";
    public static final String REQUEST_ID = "requestId";
    public static final String MALFORMED_IDENTIFIER = "for-testing";

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
    @SuppressWarnings("unchecked")
    public void requestToHandlerReturnsCustomer() throws Exception {
        UUID identifier = UUID.randomUUID();
        CustomerDb customerDb = new CustomerDb.Builder()
                .withIdentifier(identifier)
                .build();
        when(customerServiceMock.getCustomer(identifier)).thenReturn(customerDb);

        CustomerDto customerDto = customerMapper.toCustomerDto(customerDb);

        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        InputStream inputStream = new HandlerRequestBuilder<CustomerDto>(objectMapper)
            .withBody(customerDto)
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters)
            .build();
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CustomerDto> actual = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        GatewayResponse<CustomerDto> expected = new GatewayResponse<>(
            objectMapper.writeValueAsString(customerDto),
            getResponseHeaders(),
            HttpStatus.SC_OK
        );

        assertEquals(expected, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void requestToHandlerWithMalformedIdentifierReturnsBadRequest() throws Exception {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, MALFORMED_IDENTIFIER);
        InputStream inputStream = new HandlerRequestBuilder<CustomerDb>(objectMapper)
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> actual = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

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
}
