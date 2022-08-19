package no.unit.nva.customer.get;

import static java.util.Collections.singletonList;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerList;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetAllCustomersHandlerTest extends LocalCustomerServiceDatabase {
    
    private CustomerService customerService;
    private GetAllCustomersHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;
    
    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        this.setupDatabase();
        customerService = new DynamoDBCustomerService(this.dynamoClient);
        handler = new GetAllCustomersHandler(customerService);
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }
    
    @Test
    void requestToHandlerReturnsCustomerList() throws IOException, ApiGatewayException {
        
        final var savedCustomer = insertRandomCustomer();
        var input = sampleRequest();
        var response = sendRequest(input, CustomerList.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        
        CustomerList actualCustomerList = CustomerList.fromString(response.getBody());
        assertThat(actualCustomerList.getId(), notNullValue());
        assertThat(actualCustomerList.getContext(), notNullValue());
        
        CustomerList customerList = new CustomerList(singletonList(savedCustomer));
        assertThat(actualCustomerList, equalTo(customerList));
    }
    
    @Test
    void shouldReturnAListOfCustomersContainingCustomerIdCustomerDisplayNameAndCreatedDate()
        throws IOException, ApiGatewayException {
        var existingCustomer = insertRandomCustomer();
        var input = sampleRequest();
        var response = sendRequest(input, CustomerList.class);
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
    
    private <T> GatewayResponse<T> sendRequest(InputStream input, Class<T> responseType) throws java.io.IOException {
        handler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }
    
    private InputStream sampleRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper).withHeaders(getRequestHeaders()).build();
    }
    
    private CustomerDto insertRandomCustomer() throws ApiGatewayException {
        var customer = CustomerDto.builder()
                           .withDisplayName(randomString())
                           .withCristinId(randomUri())
                           .withCustomerOf(randomElement(ApplicationDomain.values()))
            .build();
        customerService.createCustomer(customer);
        return customerService.getCustomerByCristinId(customer.getCristinId());
    }
}
