package no.unit.nva.customer.get;

import static java.util.Collections.singletonList;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomUri;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerList;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetAllCustomersHandlerTest extends LocalCustomerServiceDatabase {

    private CustomerService customerService;
    private GetAllCustomersHandler handler;

    private Context context;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        this.setupDatabase();

        customerService = new DynamoDBCustomerService(this.dynamoClient);
        handler = new GetAllCustomersHandler(customerService);
        context = new FakeContext();
    }

    @Test
    void requestToHandlerReturnsCustomerList() {

        var customer =  CustomerDto.builder().withCristinId(randomUri()).build();
        customerService.createCustomer(customer);
        var savedCustomer = customerService.getCustomerByCristinId(customer.getCristinId());

        var input = new APIGatewayProxyRequestEvent().withHeaders(getRequestHeaders());

        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        CustomerList actualCustomerList = CustomerList.fromString(response.getBody());
        assertThat(actualCustomerList.getId(), notNullValue());
        assertThat(actualCustomerList.getContext(), notNullValue());

        CustomerList customerList = new CustomerList(singletonList(savedCustomer));
        assertThat(actualCustomerList, equalTo(customerList));
    }

    @Test
    void shouldReturnAListOfCustomersContainingCustomerIdCustomerDisplayNameAndCreatedDate() {

    }
}
