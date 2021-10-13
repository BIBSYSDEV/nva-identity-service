package no.unit.nva.customer.create;

import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getResponseHeaders;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;

import no.unit.nva.customer.model.CustomerDao.Builder;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        CustomerDto customerDto = new Builder()
            .withName("New Customer")
            .withIdentifier(UUID.randomUUID())
            .withVocabularySettings(Collections.emptySet())
            .build()
            .toCustomerDto();
        when(customerServiceMock.createCustomer(any(CustomerDto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        InputStream inputStream = new HandlerRequestBuilder<CustomerDto>(defaultRestObjectMapper)
            .withBody(customerDto)
            .withHeaders(getRequestHeaders())
            .build();
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CustomerDto> actual = GatewayResponse.fromOutputStream(outputStream);

        GatewayResponse<CustomerDto> expected = new GatewayResponse<>(
            defaultRestObjectMapper.writeValueAsString(customerDto),
            getResponseHeaders(),
            HttpStatus.SC_CREATED
        );

        assertEquals(expected, actual);
    }
}
