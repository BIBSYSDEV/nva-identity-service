package no.unit.nva.customer.get;

import static java.util.Collections.singletonList;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerList;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetAllCustomersHandlerTest {

    private CustomerService customerServiceMock;
    private GetAllCustomersHandler handler;

    private Context context;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        customerServiceMock = mock(CustomerService.class);
        handler = new GetAllCustomersHandler(customerServiceMock);
        context = new FakeContext();
    }

    @Test
    void requestToHandlerReturnsCustomerList() {
        UUID identifier = UUID.randomUUID();
        CustomerDao customerDb = new CustomerDao.Builder()
            .withIdentifier(identifier)
            .build();

        CustomerDto customerDto = customerDb.toCustomerDto();
        when(customerServiceMock.getCustomers()).thenReturn(singletonList(customerDto));

        var input = new APIGatewayProxyRequestEvent().withHeaders(getRequestHeaders());

        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        CustomerList actualCustomerList = CustomerList.fromString(response.getBody());
        assertThat(actualCustomerList.getId(), notNullValue());
        assertThat(actualCustomerList.getContext(), notNullValue());

        CustomerList customerList = new CustomerList(singletonList(customerDto));
        assertThat(actualCustomerList, equalTo(customerList));
    }
}
