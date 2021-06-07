package no.unit.nva.customer.update;

import static no.unit.nva.customer.testing.TestHeaders.getErrorResponseHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getResponseHeaders;
import static no.unit.nva.customer.update.UpdateCustomerHandler.IDENTIFIER;
import static nva.commons.handlers.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_REQUEST;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

public class UpdateCustomerHandlerTest {

    public static final String WILDCARD = "*";
    public static final String SAMPLE_NAMESPACE = "http://example.org/customer";
    public static final String REQUEST_ID = "requestId";

    private ObjectMapper objectMapper = ObjectMapperConfig.objectMapper;
    private CustomerMapper customerMapper;
    private CustomerService customerServiceMock;
    private Environment environmentMock;
    private UpdateCustomerHandler handler;
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
        handler = new UpdateCustomerHandler(customerServiceMock, customerMapper, environmentMock);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    public void handleRequestReturnsOkForValidRequest() throws IOException, ApiGatewayException {
        CustomerDb customerDb = createCustomerDb(UUID.randomUUID());
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDb.class))).thenReturn(customerDb);

        InputStream request = IoUtils.inputStreamFromResources(Path.of("update_request.json"));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        handler.handleRequest(request, outputStream, context);

        GatewayResponse<CustomerDto> response = GatewayResponse.fromOutputStream(outputStream);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_OK)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void requestToHandlerReturnsCustomerUpdated() throws Exception {
        UUID identifier = UUID.randomUUID();
        CustomerDb customerDb = createCustomerDb(identifier);
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDb.class))).thenReturn(customerDb);

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
            customerDto,
            getResponseHeaders(),
            HttpStatus.SC_OK
        );

        assertEquals(expected, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void requestToHandlerWithMalformedIdentifierReturnsBadRequest() throws Exception {
        String malformedIdentifier = "for-testing";
        CustomerDb customerDb = createCustomerDb(UUID.randomUUID());

        CustomerDto customerDto = customerMapper.toCustomerDto(customerDb);

        Map<String, String> pathParameters = Map.of(IDENTIFIER, malformedIdentifier);
        InputStream inputStream = new HandlerRequestBuilder<CustomerDto>(objectMapper)
            .withBody(customerDto)
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
                .withDetail(UpdateCustomerHandler.IDENTIFIER_IS_NOT_A_VALID_UUID + malformedIdentifier)
                .with(REQUEST_ID, null)
                .build(),
            getErrorResponseHeaders(),
            SC_BAD_REQUEST
        );

        assertEquals(expected, actual);
    }

    private CustomerDb createCustomerDb(UUID uuid) {
        return new CustomerDb.Builder()
            .withIdentifier(uuid)
            .withName("New Customer")
            .build();
    }
}
