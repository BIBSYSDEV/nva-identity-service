package no.unit.nva.customer.get;

import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import no.unit.nva.customer.exception.NotFoundException;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

public class GetCustomerByCristinIdHandlerTest {

    public static final String SAMPLE_CRISTIN_ID = "http://cristin.id";
    public static final String WILDCARD = "*";
    private GetCustomerByCristinIdHandler handler;
    private CustomerService customerService;
    private Environment environment;
    private ByteArrayOutputStream outputStream;
    private Context context;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void init() {
        customerService = mock(CustomerService.class);
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        handler = new GetCustomerByCristinIdHandler(customerService, environment);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    public void handleRequestReturnsExistingCustomerOnValidCristinId() throws Exception {
        prepareMocksWithExistingCustomer();

        Map<String, String> pathParameters = Map.of(GetCustomerByCristinIdHandler.CRISTIN_ID, SAMPLE_CRISTIN_ID);
        InputStream inputStream = new HandlerRequestBuilder<Void>(defaultRestObjectMapper)
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CustomerDto> response = GatewayResponse.fromOutputStream(outputStream);

        assertNotNull(response.getBodyObject(CustomerDto.class));
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    }

    @Test
    public void handleRequestReturnsNotFoundOnInvalidCristinId() throws Exception {
        prepareMocksWithMissingCustomer();

        Map<String, String> pathParameters = Map.of(GetCustomerByCristinIdHandler.CRISTIN_ID, SAMPLE_CRISTIN_ID);
        InputStream inputStream = new HandlerRequestBuilder<Void>(defaultRestObjectMapper)
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);

        assertNotNull(response.getBodyObject(Problem.class));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
    }

    private void prepareMocksWithMissingCustomer() throws ApiGatewayException {
        when(customerService.getCustomerByCristinId(SAMPLE_CRISTIN_ID)).thenThrow(NotFoundException.class);
    }

    private void prepareMocksWithExistingCustomer() throws ApiGatewayException {
        when(customerService.getCustomerByCristinId(SAMPLE_CRISTIN_ID)).thenReturn(createCustomer());
    }

    private CustomerDto createCustomer() {
        CustomerDao customer = new CustomerDao.Builder()
            .withIdentifier(UUID.randomUUID())
            .withCristinId(SAMPLE_CRISTIN_ID)
            .build();
        return customer.toCustomerDto();
    }
}