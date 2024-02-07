package no.unit.nva.customer.update;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.update.UpdateCustomerHandler.IDENTIFIER;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class UpdateCustomerHandlerTest {

    private CustomerService customerServiceMock;
    private UpdateCustomerHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private final URI testServiceCenterUri = randomUri();

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        customerServiceMock = mock(CustomerService.class);

        handler = new UpdateCustomerHandler(customerServiceMock);
        outputStream = new ByteArrayOutputStream();
        context = new FakeContext();
    }

    @Test
    void shouldReturnOkForValidRequest() throws IOException, InputException, NotFoundException {
        CustomerDto customer = createCustomer(UUID.randomUUID());
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);

        var request = createInput(customer, Map.of("identifier", "b8c3e125-cadb-43d5-823a-2daa7768c3f9"));

        var response = sendRequest(request, CustomerDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldReturnForbiddenWhenNoAccess() throws InputException, NotFoundException, IOException {
        CustomerDto customer = createCustomer(UUID.randomUUID());
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);

        var request = createInputWithoutAccessRights(customer, Map.of());

        var response = sendRequest(request, CustomerDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void requestToHandlerReturnsCustomerUpdated() throws InputException, NotFoundException, IOException {
        UUID identifier = UUID.randomUUID();
        CustomerDto customer = createCustomer(identifier);
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);

        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        var input = createInput(customer, pathParameters);

        var response = sendRequest(input, CustomerDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE), is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    @Test
    void requestToHandlerReturnsCustomerInactiveUpdated() throws InputException, NotFoundException, IOException {
        UUID identifier = UUID.randomUUID();
        CustomerDto customer = createCustomer(identifier);
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);
        assertThat(customer.getInactiveFrom(), is(nullValue()));

        var now = Instant.now();
        customer.setInactiveFrom(now);
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);
        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        var input = createInput(customer, pathParameters);

        var response = sendRequest(input, CustomerDto.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        verify(customerServiceMock, times(1)).updateCustomer(any(UUID.class), eq(customer));
    }

    @Test
    void requestToHandlerReturnsCustomerServiceCenterUriUpdated()
        throws InputException, NotFoundException, IOException {
        UUID identifier = UUID.randomUUID();
        CustomerDto customer = createCustomer(identifier);
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);
        assertThat(customer.getServiceCenterUri(), is(nullValue()));

        customer.setServiceCenterUri(testServiceCenterUri);
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);
        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        var input = createInput(customer, pathParameters);

        sendRequest(input, CustomerDto.class);
        assertThat(customer.getServiceCenterUri(), is(equalTo(testServiceCenterUri)));
        verify(customerServiceMock, times(1)).updateCustomer(any(UUID.class), eq(customer));
    }

    @Test
    void requestToHandlerWithMalformedIdentifierReturnsBadRequest() throws IOException {
        String malformedIdentifier = "for-testing";
        CustomerDto customer = createCustomer(UUID.randomUUID());

        Map<String, String> pathParameters = Map.of(IDENTIFIER, malformedIdentifier);
        var request = createInput(customer, pathParameters);

        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(response.getBody(),
                   containsString(UpdateCustomerHandler.IDENTIFIER_IS_NOT_A_VALID_UUID + malformedIdentifier));
    }

    @Test
    void shouldReturnBadRequestForMalformedObject() throws IOException {

        var pathParameters = Map.of(IDENTIFIER, UUID.randomUUID().toString());
        var request = new HandlerRequestBuilder<String>(dtoObjectMapper).withBody(randomString())
                          .withHeaders(getRequestHeaders())
                          .withPathParameters(pathParameters)
                          .build();

        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    //TODO
    @Test
    void shouldReturnPublicationWorkflowWhenValueIsSet() {

    }

    //TODO
    @Test
    void shouldReturnDefaultPublicationWorkflowWhenNoneIsSet() {
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream createInput(CustomerDto customer, Map<String, String> pathParameters)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper).withBody(customer)
                   .withHeaders(getRequestHeaders())
                   .withAccessRights(randomUri(), MANAGE_CUSTOMERS)
                   .withPathParameters(pathParameters)
                   .build();
    }

    private InputStream createInputWithoutAccessRights(CustomerDto customer, Map<String, String> pathParameters)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper).withBody(customer)
                   .withHeaders(getRequestHeaders())
                   .withPathParameters(pathParameters)
                   .build();
    }

    private CustomerDto createCustomer(UUID uuid) {
        return CustomerDto.builder()
                   .withIdentifier(uuid)
                   .withName("New Customer")
                   .withCustomerOf(randomElement(ApplicationDomain.values()))
                   .build();
    }
}
