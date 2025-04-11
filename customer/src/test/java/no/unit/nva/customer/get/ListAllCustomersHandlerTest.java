package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerList;
import no.unit.nva.customer.model.interfaces.DoiAgent;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import static java.util.Collections.singletonList;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

class ListAllCustomersHandlerTest extends LocalCustomerServiceDatabase {

    private CustomerService customerService;
    private ListAllCustomersHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        this.setupDatabase();
        customerService = new DynamoDBCustomerService(this.dynamoClient);
        handler = new ListAllCustomersHandler(customerService, new Environment());
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void requestToHandlerReturnsCustomerList() throws IOException, ApiGatewayException {

        final var savedCustomer = insertRandomCustomer();
        var input = sampleRequestWithAccess();
        var response = sendRequest(input, CustomerList.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        CustomerList actualCustomerList = CustomerList.fromString(response.getBody());
        assertThat(actualCustomerList.getId(), notNullValue());
        assertThat(actualCustomerList.getContext(), notNullValue());

        CustomerList customerList = new CustomerList(singletonList(savedCustomer));
        assertThat(actualCustomerList, equalTo(customerList));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream input, Class<T> responseType) throws java.io.IOException {
        handler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream sampleRequestWithAccess() throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper)
            .withAccessRights(randomUri(), AccessRight.MANAGE_CUSTOMERS)
            .withHeaders(getRequestHeaders())
            .build();
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

    @Test
    void requestToHandlerReturnsForbiddenWhenNotAppAdmin() throws IOException {
        var input = sampleRequestWithoutAccess();
        var response = sendRequest(input, CustomerList.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    private InputStream sampleRequestWithoutAccess() throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper)
            .withHeaders(getRequestHeaders())
            .build();
    }

    @Test
    void shouldReturnAListOfCustomersContainingCustomerIdCustomerDisplayNameAndCreatedDate()
        throws IOException, ApiGatewayException {
        var existingCustomer = insertRandomCustomer();
        var input = sampleRequestWithAccess();
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

    @Test
    void shouldReturnAListOfCustomersContainingCustomerDoiPrefix()
        throws ConflictException, NotFoundException, IOException {
        var doiPrefix = randomString();
        insertRandomCustomerWithDoiPrefix(doiPrefix);
        var input = sampleRequestWithAccess();
        var response = sendRequest(input, CustomerList.class);
        var customerList = CustomerList.fromString(response.getBody());
        assertThat(customerList.getCustomers(), hasSize(1));
        var actualCustomerPreview = customerList.getCustomers().get(0);
        assertThat(actualCustomerPreview.getDoiPrefix(), is(equalTo(doiPrefix)));
    }

    private CustomerDto insertRandomCustomerWithDoiPrefix(String doiPrefix)
        throws ConflictException, NotFoundException {
        var customer = CustomerDto.builder()
            .withDisplayName(randomString())
            .withCristinId(randomUri())
            .withCustomerOf(randomElement(ApplicationDomain.values()))
            .withDoiAgent(createDoiAgent(doiPrefix))
            .build();
        customerService.createCustomer(customer);
        return customerService.getCustomerByCristinId(customer.getCristinId());
    }

    private DoiAgent createDoiAgent(String doiPrefix) {
        var doiAgent = new CustomerDto.DoiAgentDto();
        doiAgent.setPrefix(doiPrefix);
        doiAgent.setId(randomUri());
        doiAgent.setUsername(randomString());
        return doiAgent;
    }
}
