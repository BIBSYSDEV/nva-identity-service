package no.unit.nva.customer.create;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;

import static no.unit.nva.customer.model.PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.customer.model.PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getResponseHeaders;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class CreateCustomerHandlerTest extends LocalCustomerServiceDatabase {

    private CreateCustomerHandler handler;
    private Context context;

    @BeforeEach
    public void setUp() {
        super.setupDatabase();
        CustomerService customerServiceMock = new DynamoDBCustomerService(this.dynamoClient);
        handler = new CreateCustomerHandler(customerServiceMock);
        context = new FakeContext();
    }

    @AfterEach
    public void close() {
        super.deleteDatabase();
    }

    @Test
    void requestToHandlerReturnsCustomerCreated() {
        var requestBody = CreateCustomerRequest.fromCustomerDto(validCustomerDto());
        var response = executeRequest(requestBody);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        assertThat(response.getHeaders(), is(equalTo(getResponseHeaders())));

        var actualBody = CustomerDto.fromJson(response.getBody());
        var expectedPersistedInformation = CreateCustomerRequest.fromCustomerDto(actualBody);
        assertThat(expectedPersistedInformation, is(equalTo(requestBody)));
    }

    @Test
    void shouldReturnDefaultPublicationWorkflowWhenNoneIsSet() {
        var requestBody = CreateCustomerRequest.fromCustomerDto(validCustomerDto());
        var response = executeRequest(requestBody);
        var actualResponseBody = CustomerDto.fromJson(response.getBody());
        assertThat(actualResponseBody.getPublicationWorkflow(), is(equalTo(REGISTRATOR_PUBLISHES_METADATA_AND_FILES)));
    }

    @Test
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
    }

    @Test
    void shouldReturnBadRequestWhenInputIsNotAValidJson() {
        var input = new APIGatewayProxyRequestEvent()
            .withBody(randomString())
            .withHeaders(getRequestHeaders());
        var response = handler.handleRequest(input, context);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    void shouldReturnBadRequestWhenInputIsNotAValidCustomerRequest() {
        var body = Map.of("type", randomString());
        var input = new APIGatewayProxyRequestEvent()
            .withBody(attempt(() -> JsonConfig.writeValueAsString(body)).orElseThrow())
            .withHeaders(getRequestHeaders());
        var response = handler.handleRequest(input, context);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    private APIGatewayProxyResponseEvent executeRequest(CreateCustomerRequest request) {
        var input = new APIGatewayProxyRequestEvent()
                .withBody(request.toString())
                .withHeaders(getRequestHeaders());
        return handler.handleRequest(input, context);
    }

    private CustomerDto validCustomerDto() {
        return CustomerDto.builder()
                .withName("New Customer")
                .withVocabularies(Collections.emptySet())
                .build();
    }
}
