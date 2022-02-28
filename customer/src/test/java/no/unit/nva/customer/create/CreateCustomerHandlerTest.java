package no.unit.nva.customer.create;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.responses.CustomerResponse;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getResponseHeaders;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateCustomerHandlerTest {

    public static final String WILDCARD = "*";

    private CustomerService customerServiceMock;
    private CreateCustomerHandler handler;
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
        handler = new CreateCustomerHandler(customerServiceMock, environmentMock);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    public void requestToHandlerReturnsCustomerCreated() throws Exception {
        CustomerDto customerDto = CustomerDataGenerator.createSampleCustomerDto();
        CustomerDao customerDao = CustomerDao.fromCustomerDto(customerDto);
        when(customerServiceMock.createCustomer(any(CustomerDao.class)))
            .thenReturn(customerDao);

        InputStream inputStream = new HandlerRequestBuilder<CustomerDto>(defaultRestObjectMapper)
            .withBody(customerDto)
            .withHeaders(getRequestHeaders())
            .build();
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CustomerResponse> actual = GatewayResponse.fromOutputStream(outputStream);

        GatewayResponse<CustomerResponse> expected = new GatewayResponse<>(
            defaultRestObjectMapper.writeValueAsString(CustomerResponse.toCustomerResponse(customerDao)),
            getResponseHeaders(),
            HttpStatus.SC_CREATED
        );

        assertEquals(expected, actual);
    }
}
