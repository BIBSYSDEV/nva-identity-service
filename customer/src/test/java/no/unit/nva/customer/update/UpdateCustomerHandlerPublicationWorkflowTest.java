package no.unit.nva.customer.update;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.model.PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.customer.model.PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.update.UpdateCustomerHandler.IDENTIFIER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.create.CreateCustomerHandler;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateCustomerHandlerPublicationWorkflowTest extends LocalCustomerServiceDatabase {

    private static final Instant NOW = Instant.now();
    private UpdateCustomerHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;
    private CustomerDto preExistingCustomer;
    private CreateCustomerHandler createHandler;

    @BeforeEach
    public void setUp() {
        super.setupDatabase();
        var customerServiceMock = new DynamoDBCustomerService(getDynamoClient());
        handler = new UpdateCustomerHandler(customerServiceMock);
        createHandler = new CreateCustomerHandler(customerServiceMock);
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    public void close() {
        super.deleteDatabase();
    }

    @Test
    void shouldUpdatePublicationWorkflowWhenAuthorized() throws IOException {
        createCustomerInLocalDb();
        var updateCustomer = changePublicationWorkflowFromPreExistingCustomer();

        assertThat(updateCustomer.getPublicationWorkflow(),
                   is(not(equalTo(preExistingCustomer.getPublicationWorkflow()))));

        var request = updateRequestWithCorrectAuthorization(updateCustomer,
                                                            getIdentifierPathParam(updateCustomer));
        var response = sendUpdateRequest(request, CustomerDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getBodyObject(CustomerDto.class).getPublicationWorkflow(),
                   is(equalTo(updateCustomer.getPublicationWorkflow())));
    }

    @Test
    void shouldThrowForbiddenWhenTryingToUpdatePublicationWorkflowWhenNotAuthorized() throws IOException {
        createCustomerInLocalDb();
        var updateCustomer = changePublicationWorkflowFromPreExistingCustomer();
        var request = updateRequestWithoutCorrectAuthorization(updateCustomer,
                                                               getIdentifierPathParam(updateCustomer));
        var response = sendUpdateRequest(request, CustomerDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldStillAllowUpdateOfOtherFieldsThanPublicationWorkflow() throws IOException {
        createCustomerInLocalDb();
        var updateCustomer = preExistingCustomer.copy().withDisplayName(randomString()).build();
        var request = updateRequestWithoutCorrectAuthorization(updateCustomer,
                                                               getIdentifierPathParam(updateCustomer));
        var response = sendUpdateRequest(request, CustomerDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    private Map<String, String> getIdentifierPathParam(CustomerDto customerDto) {
        return Map.of(IDENTIFIER, customerDto.getIdentifier().toString());
    }

    private CustomerDto changePublicationWorkflowFromPreExistingCustomer() {
        return preExistingCustomer
                   .copy()
                   .withPublicationWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY)
                   .build();
    }

    private InputStream updateRequestWithCorrectAuthorization(CustomerDto updateCustomer,
                                                              Map<String, String> pathParameters)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper)
                   .withPathParameters(pathParameters)
                   .withCustomerId(updateCustomer.getId())
                   .withAccessRights(updateCustomer.getId(),
                                     AccessRight.EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW.toString())
                   .withBody(updateCustomer)
                   .build();
    }

    private InputStream updateRequestWithoutCorrectAuthorization(CustomerDto updateCustomer,
                                                                 Map<String, String> pathParameters)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper)
                   .withPathParameters(pathParameters)
                   .withBody(updateCustomer)
                   .build();
    }

    private void createCustomerInLocalDb() throws IOException {
        var createCustomerBase = createCustomer(UUID.randomUUID());
        var createOutputStream = new ByteArrayOutputStream();
        createHandler.handleRequest(createCustomerInDbRequest(createCustomerBase),
                createOutputStream,
                new FakeContext());

        preExistingCustomer = GatewayResponse.fromOutputStream(createOutputStream, CustomerDto.class)
                                  .getBodyObject(CustomerDto.class);
    }

    private InputStream createCustomerInDbRequest(CustomerDto customer)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper)
                   .withBody(customer)
                   .withHeaders(getRequestHeaders())
                   .build();
    }

    private CustomerDto createCustomer(UUID uuid) {
        return new CustomerDao.Builder()
                   .withIdentifier(uuid)
                   .withName("New Customer")
                   .withCreatedDate(NOW)
                   .withModifiedDate(NOW)
                   .withPublicationWorkflow(REGISTRATOR_PUBLISHES_METADATA_AND_FILES)
                   .build()
                   .toCustomerDto();
    }

    private <T> GatewayResponse<T> sendUpdateRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }
}
