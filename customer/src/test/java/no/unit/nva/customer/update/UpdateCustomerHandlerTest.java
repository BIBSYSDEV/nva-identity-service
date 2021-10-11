package no.unit.nva.customer.update;

import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
import static no.unit.nva.customer.testing.TestHeaders.getErrorResponseHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getResponseHeaders;
import static no.unit.nva.customer.update.UpdateCustomerHandler.IDENTIFIER;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

public class UpdateCustomerHandlerTest {

    public static final String WILDCARD = "*";
    public static final String REQUEST_ID = "requestId";

    private CustomerService customerServiceMock;
    private UpdateCustomerHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        customerServiceMock = mock(CustomerService.class);
        Environment environmentMock = mock(Environment.class);
        when(environmentMock.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        handler = new UpdateCustomerHandler(customerServiceMock, environmentMock);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    public void handleRequestReturnsOkForValidRequest() throws IOException, ApiGatewayException {
        CustomerDto customer = createCustomer(UUID.randomUUID());
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);

        InputStream request = IoUtils.inputStreamFromResources("update_request.json");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        handler.handleRequest(request, outputStream, context);

        GatewayResponse<CustomerDto> response = GatewayResponse.fromOutputStream(outputStream);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_OK)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void requestToHandlerReturnsCustomerUpdated() throws Exception {
        UUID identifier = UUID.randomUUID();
        CustomerDto customer = createCustomer(identifier);
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);

        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        InputStream inputStream = new HandlerRequestBuilder<CustomerDto>(defaultRestObjectMapper)
            .withBody(customer)
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CustomerDto> actual = defaultRestObjectMapper.readValue(
            outputStream.toByteArray(),
            GatewayResponse.class);

        GatewayResponse<CustomerDto> expected = new GatewayResponse<>(
            defaultRestObjectMapper.writeValueAsString(customer),
            getResponseHeaders(),
            HttpStatus.SC_OK
        );

        assertEquals(expected, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void requestToHandlerWithMalformedIdentifierReturnsBadRequest() throws Exception {
        String malformedIdentifier = "for-testing";
        CustomerDto customer = createCustomer(UUID.randomUUID());


        Map<String, String> pathParameters = Map.of(IDENTIFIER, malformedIdentifier);
        InputStream inputStream = new HandlerRequestBuilder<CustomerDto>(defaultRestObjectMapper)
            .withBody(customer)
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> actual = defaultRestObjectMapper.readValue(
            outputStream.toByteArray(),
            GatewayResponse.class);

        Problem problem = Problem.builder()
                .withStatus(BAD_REQUEST)
                .withTitle(BAD_REQUEST.getReasonPhrase())
                .withDetail(UpdateCustomerHandler.IDENTIFIER_IS_NOT_A_VALID_UUID + malformedIdentifier)
                .with(REQUEST_ID, null)
                .build();

        GatewayResponse<Problem> expected = new GatewayResponse<>(
            defaultRestObjectMapper.writeValueAsString(problem),
            getErrorResponseHeaders(),
            SC_BAD_REQUEST
        );

        assertEquals(expected, actual);
    }

    private CustomerDto createCustomer(UUID uuid) {
        return new CustomerDao.Builder()
            .withIdentifier(uuid)
            .withName("New Customer")
            .build()
            .toCustomerDto();
    }
}
