package no.unit.nva.customer.update;

import static no.unit.nva.customer.model.PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.customer.model.PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.customer.testing.TestHeaders.getMultiValuedHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.update.UpdateCustomerHandler.IDENTIFIER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.create.CreateCustomerRequest;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.stubs.FakeContext;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateCustomerHandlerTest {

    public static final String WILDCARD = "*";
    public static final String REQUEST_ID = "requestId";

    private CustomerService customerServiceMock;
    private UpdateCustomerHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;

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
    void shouldReturnOkForValidRequest() {
        CustomerDto customer = createCustomer(UUID.randomUUID());
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);

        var request = new APIGatewayProxyRequestEvent()
            .withPathParameters(Map.of("identifier", "b8c3e125-cadb-43d5-823a-2daa7768c3f9"))
            .withBody(stringFromResources(Path.of("update_request.json")));

        var response = handler.handleRequest(request, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void requestToHandlerReturnsCustomerUpdated() {
        UUID identifier = UUID.randomUUID();
        CustomerDto customer = createCustomer(identifier);
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);

        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        var input = createInput(customer, pathParameters);

        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE), is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    @Test
    void requestToHandlerWithMalformedIdentifierReturnsBadRequest() {
        String malformedIdentifier = "for-testing";
        CustomerDto customer = createCustomer(UUID.randomUUID());

        Map<String, String> pathParameters = Map.of(IDENTIFIER, malformedIdentifier);
        var request = createInput(customer, pathParameters);

        var response = handler.handleRequest(request, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(response.getBody(),
                   containsString(UpdateCustomerHandler.IDENTIFIER_IS_NOT_A_VALID_UUID + malformedIdentifier));
    }

    @Test
    void shouldReturnBadRequestForMalformedObject() {

        var pathParameters = Map.of(IDENTIFIER, UUID.randomUUID().toString());
        var request = new APIGatewayProxyRequestEvent()
            .withBody(randomString())
            .withHeaders(getRequestHeaders())
            .withMultiValueHeaders(getMultiValuedHeaders())
            .withPathParameters(pathParameters);

        var response = handler.handleRequest(request, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    void shouldReturnDefaultPublicationWorkflowWhenNoneIsSet() {
        UUID identifier = UUID.randomUUID();
        CustomerDto customer = createCustomer(identifier);
        customer.setPublicationWorkflow(null);
        when(customerServiceMock.updateCustomer(any(UUID.class), any(CustomerDto.class))).thenReturn(customer);

        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        var input = createInput(customer, pathParameters);

        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE), is(equalTo(MediaType.JSON_UTF_8.toString())));

        var responseBody = CustomerDto.fromJson(response.getBody());

        assertThat(responseBody.getPublicationWorkflow(), is(equalTo(REGISTRATOR_PUBLISHES_METADATA_AND_FILES)));
    }

    /*@Test
    void shouldReturnPublicationWorkflowWhenValueIsSet() {
        var customerDto = CustomerDto.builder()
                              .withName("New Customer")
                              .withVocabularies(Collections.emptySet())
                              .withPublicationWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY)
                              .build();
        var requestBody = CreateCustomerRequest.fromCustomerDto(customerDto);
        var response = executeRequest(requestBody);
        var actualResponseBody = CustomerDto.fromJson(response.getBody());
        assertThat(actualResponseBody.getPublicationWorkflow(), is(equalTo(REGISTRATOR_PUBLISHES_METADATA_ONLY)));
    }*/

    private APIGatewayProxyRequestEvent createInput(CustomerDto customer, Map<String, String> pathParameters) {
        return new APIGatewayProxyRequestEvent()
            .withBody(customer.toString())
            .withHeaders(getRequestHeaders())
            .withMultiValueHeaders(getMultiValuedHeaders())
            .withPathParameters(pathParameters);
    }

    private CustomerDto createCustomer(UUID uuid) {
        return new CustomerDao.Builder()
            .withIdentifier(uuid)
            .withName("New Customer")
            .build()
            .toCustomerDto();
    }
}
