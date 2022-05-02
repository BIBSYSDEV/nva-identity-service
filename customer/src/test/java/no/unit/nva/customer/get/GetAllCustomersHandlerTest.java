package no.unit.nva.customer.get;

import static java.util.Collections.singletonList;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
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

        final var savedCustomer = insertRandomCustomer();
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
        var existingCustomer = insertRandomCustomer();
        var input = new APIGatewayProxyRequestEvent().withHeaders(getRequestHeaders());
        var response = handler.handleRequest(input, context);
        var customerList = CustomerList.fromString(response.getBody());
        assertThat(customerList.getId(), notNullValue());
        assertThat(customerList.getContext(), notNullValue());

        for (var customer : customerList.getCustomers()) {
            assertThat(customer.getDisplayName(), is(equalTo(existingCustomer.getDisplayName())));
            assertThat(customer.getId(), is(equalTo(existingCustomer.getId())));
            assertThat(customer.getCreatedDate(), is(equalTo(existingCustomer.getCreatedDate())));
            assertThat(customer, is(not(instanceOf(CustomerDto.class))));
        }
    }

    private CustomerDto insertRandomCustomer() {
        var customer = CustomerDto.builder()
            .withDisplayName(randomString())
            .withCristinId(randomUri())
            .build();
        customerService.createCustomer(customer);
        return customerService.getCustomerByCristinId(customer.getCristinId());
    }
}
