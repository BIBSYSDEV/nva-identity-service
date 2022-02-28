package no.unit.nva.customer.get;

import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

public class GetCustomerByOrgNumberHandlerTest {

    public static final String WILDCARD = "*";
    public static final String REQUEST_ID = "requestId";
    public static final String SAMPLE_ORG_NUMBER = "123";
    public static final String EXPECTED_ERROR_MESSAGE = "Missing from pathParameters: orgNumber";
    public static final String SAMPLE_CRISTIN_ID = "https://cristin.id";

    private CustomerService customerServiceMock;
    private GetCustomerByOrgNumberHandler handler;
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
        handler = new GetCustomerByOrgNumberHandler(customerServiceMock, environmentMock);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getCustomerByOrgNumberReturnsCustomerWhenInputIsExistingCustomerOrgNumber() throws Exception {
        CustomerDao customerDb = CustomerDataGenerator.createSampleCustomerDao();
        when(customerServiceMock.getCustomerByOrgNumber(SAMPLE_ORG_NUMBER)).thenReturn(customerDb);

        Map<String, String> pathParameters = Map.of(GetCustomerByOrgNumberHandler.ORG_NUMBER, SAMPLE_ORG_NUMBER);
        InputStream inputStream = new HandlerRequestBuilder<Void>(defaultRestObjectMapper)
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters)
            .build();
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CustomerIdentifiers> actual = defaultRestObjectMapper.readValue(
            outputStream.toByteArray(),
            GatewayResponse.class);

        GatewayResponse<CustomerIdentifiers> expected = new GatewayResponse<>(
                defaultRestObjectMapper.writeValueAsString(
                new CustomerIdentifiers(customerDb.toCustomerDto().getId(),
                                        URI.create(customerDb.getCristinId()))),
            getResponseHeaders(),
            HttpStatus.SC_OK
        );

        assertEquals(expected, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getCustomerByOrgNumberReturnsBadRequestWhenOrgNumberisNull() throws Exception {

        InputStream inputStream = new HandlerRequestBuilder<Void>(defaultRestObjectMapper)
            .withHeaders(getRequestHeaders())
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> actual = defaultRestObjectMapper.readValue(
            outputStream.toByteArray(),
            GatewayResponse.class);

        ThrowableProblem problem = Problem.builder()
                .withStatus(BAD_REQUEST)
                .withTitle(BAD_REQUEST.getReasonPhrase())
                .withDetail(EXPECTED_ERROR_MESSAGE)
                .with(REQUEST_ID, null)
                .build();

        GatewayResponse<Problem> expected = new GatewayResponse<>(
            defaultRestObjectMapper.writeValueAsString(problem),
            getErrorResponseHeaders(),
            SC_BAD_REQUEST
        );

        assertEquals(expected, actual);
    }
}
