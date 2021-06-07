package no.unit.nva.customer.create;

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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.mockito.Mockito;

import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getResponseHeaders;
import static nva.commons.handlers.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateCustomerHandlerTest {

    public static final String WILDCARD = "*";
    public static final String SAMPLE_NAMESPACE = "http://example.org/customer";

    private ObjectMapper objectMapper = ObjectMapperConfig.objectMapper;
    private CustomerMapper customerMapper;
    private CustomerService customerServiceMock;
    private Environment environmentMock;
    private CreateCustomerHandler handler;
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
        handler = new CreateCustomerHandler(customerServiceMock, customerMapper, environmentMock);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void requestToHandlerReturnsCustomerCreated() throws Exception {
        CustomerDb customerDb = new CustomerDb.Builder()
                .withName("New Customer")
                .build();
        when(customerServiceMock.createCustomer(customerDb)).thenReturn(customerDb);

        InputStream inputStream = new HandlerRequestBuilder<CustomerDb>(objectMapper)
            .withBody(customerDb)
            .withHeaders(getRequestHeaders())
            .build();
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CustomerDto> actual = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        GatewayResponse<CustomerDto> expected = new GatewayResponse<>(
                customerMapper.toCustomerDto(customerDb),
                getResponseHeaders(),
                HttpStatus.SC_CREATED
        );

        assertEquals(expected, actual);
    }
}
