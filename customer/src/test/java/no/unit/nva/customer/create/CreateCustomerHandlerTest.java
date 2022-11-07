package no.unit.nva.customer.create;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.model.PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.customer.model.PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.customer.model.interfaces.DoiAgent.randomDoiAgent;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getResponseHeaders;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class CreateCustomerHandlerTest extends LocalCustomerServiceDatabase {
    
    private CreateCustomerHandler handler;
    private Context context;
    private ByteArrayOutputStream outputSteam;
    
    @BeforeEach
    public void setUp() {
        super.setupDatabase();
        CustomerService customerServiceMock = new DynamoDBCustomerService(this.dynamoClient);
        handler = new CreateCustomerHandler(customerServiceMock);
        context = new FakeContext();
        outputSteam = new ByteArrayOutputStream();
    }
    
    @AfterEach
    public void close() {
        super.deleteDatabase();
    }
    
    @Test
    void requestToHandlerReturnsCustomerCreated() throws BadRequestException, IOException {
        var requestBody = CreateCustomerRequest.fromCustomerDto(validCustomerDto());
        var response = executeRequest(requestBody, CustomerDto.class);
        
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        assertThat(response.getHeaders(), is(equalTo(getResponseHeaders())));
        
        var actualBody = CustomerDto.fromJson(response.getBody());
        var expectedPersistedInformation = CreateCustomerRequest.fromCustomerDto(actualBody);
        assertThat(expectedPersistedInformation, is(equalTo(requestBody)));
    }
    
    @Test
    void shouldReturnDefaultPublicationWorkflowWhenNoneIsSet() throws BadRequestException, IOException {
        var requestBody = CreateCustomerRequest.fromCustomerDto(validCustomerDto());
        var response = executeRequest(requestBody, CustomerDto.class);
        var actualResponseBody = CustomerDto.fromJson(response.getBody());
        assertThat(actualResponseBody.getPublicationWorkflow(), is(equalTo(REGISTRATOR_PUBLISHES_METADATA_AND_FILES)));
    }
    
    @Test
    void shouldReturnPublicationWorkflowWhenValueIsSet() throws BadRequestException, IOException {
        var customerDto = CustomerDto.builder()
                              .withName("New Customer")
                              .withVocabularies(Collections.emptySet())
                              .withPublicationWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY)
                              .withCustomerOf(randomElement(ApplicationDomain.values()))
            .build();
        var requestBody = CreateCustomerRequest.fromCustomerDto(customerDto);
        var response = executeRequest(requestBody, CustomerDto.class);
        var actualResponseBody = CustomerDto.fromJson(response.getBody());
        assertThat(actualResponseBody.getPublicationWorkflow(), is(equalTo(REGISTRATOR_PUBLISHES_METADATA_ONLY)));
    }
    
    @Test
    void shouldReturnPublicationWorkflowErrorWhenValueIsWrong() throws IOException {
        var customerDto = CustomerDto.builder()
            .withName("New Customer")
            .withVocabularies(Collections.emptySet())
            .withPublicationWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY)
            .build();
        var requestBody = CreateCustomerRequest.fromCustomerDto(customerDto).toString()
            .replace(REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue(), "hello");
        var response = executeRequest(requestBody, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }
    
    @Test
    void shouldReturnBadRequestWhenInputIsNotAValidJson() throws IOException {
        var response = executeRequest(randomString(), Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }
    
    @Test
    void shouldReturnBadRequestWhenInputIsNotAValidCustomerRequest() throws IOException {
        var body = Map.of("type", randomString());
        var input = new HandlerRequestBuilder<String>(dtoObjectMapper)
            .withBody(attempt(() -> JsonConfig.writeValueAsString(body)).orElseThrow())
            .withHeaders(getRequestHeaders());
        var response = executeRequest(input, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }
    
    @Test
    void shouldReturnConflictErrorWhenTryingToCreateCustomerForInstitutionThatIsAleadyCustomer() throws IOException {
        var requestBody = CreateCustomerRequest.fromCustomerDto(validCustomerDto());
        executeRequest(requestBody, CustomerDto.class);
        var response = insertCustomerWithSameInstitutionId(requestBody);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
    }
    
    private GatewayResponse<CustomerDto> insertCustomerWithSameInstitutionId(CreateCustomerRequest requestBody)
        throws IOException {
        return executeRequest(requestBody, CustomerDto.class);
    }
    
    private <I, O> GatewayResponse<O> executeRequest(I request, Class<O> responseType)
        throws IOException {
        outputSteam = new ByteArrayOutputStream();
        var input = new HandlerRequestBuilder<I>(dtoObjectMapper)
            .withBody(request)
            .withHeaders(getRequestHeaders())
            .build();
        handler.handleRequest(input, outputSteam, context);
        return GatewayResponse.fromOutputStream(outputSteam, responseType);
    }
    
    private CustomerDto validCustomerDto() {
        return CustomerDto.builder()
                   .withName("New Customer")
                   .withCristinId(randomUri())
                   .withVocabularies(Collections.emptySet())
                   .withCustomerOf(randomElement(ApplicationDomain.values()))
                   .withDoiAgent(randomDoiAgent(randomString()))
            .build();
    }
}
