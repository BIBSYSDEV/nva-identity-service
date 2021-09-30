package no.unit.nva.customer.get;

import static java.util.Collections.singletonList;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerList;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class GetAllCustomersHandlerTest {

    public static final String WILDCARD = "*";

    private final ObjectMapper objectMapper = JsonUtils.objectMapper;
    private CustomerService customerServiceMock;
    private GetAllCustomersHandler handler;
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
        handler = new GetAllCustomersHandler(customerServiceMock, environmentMock);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void requestToHandlerReturnsCustomerList() throws Exception {
        UUID identifier = UUID.randomUUID();
        CustomerDb customerDb = new CustomerDb.Builder()
                .withIdentifier(identifier)
                .build();

        CustomerDto customerDto = customerDb.toCustomerDto();
        when(customerServiceMock.getCustomers()).thenReturn(singletonList(customerDto));

        InputStream inputStream = new HandlerRequestBuilder<Void>(objectMapper)
                .withHeaders(getRequestHeaders())
                .build();

        handler.handleRequest(inputStream, outputStream, context);

        String outputString = outputStream.toString();
        GatewayResponse<CustomerList> actual = GatewayResponse.fromOutputStream(outputStream);

        assertEquals(HttpStatus.SC_OK, actual.getStatusCode());
        CustomerList actualCustomerList = actual.getBodyObject(CustomerList.class);
        assertThat(actualCustomerList.getId(), notNullValue());
        assertThat(actualCustomerList.getContext(), notNullValue());

        CustomerList customerList = new CustomerList(singletonList(customerDto));
        assertThat(actualCustomerList, equalTo(customerList));
    }
}
