package no.unit.nva.customer.update;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.model.PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.customer.model.PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.update.UpdateCustomerHandler.IDENTIFIER;
import static no.unit.nva.testutils.HandlerRequestBuilder.SCOPE_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
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
import no.unit.nva.customer.model.ApplicationDomain;
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
    public static final String AWS_COGNITO_SIGNIN_USER_ADMIN = "aws.cognito.signin.user.admin";
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

        var request = updateRequestWithCorrectAuthorization(updateCustomer, getIdentifierPathParam(updateCustomer));
        var response = sendUpdateRequest(request, CustomerDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getBodyObject(CustomerDto.class).getPublicationWorkflow(),
                   is(equalTo(updateCustomer.getPublicationWorkflow())));
    }

    @Test
    void shouldUpdatePublicationWorkflowWhenAuthorizedAsCognitoAdmin() throws IOException {
        createCustomerInLocalDb();
        var updateCustomer = changePublicationWorkflowFromPreExistingCustomer();

        assertThat(updateCustomer.getPublicationWorkflow(),
                   is(not(equalTo(preExistingCustomer.getPublicationWorkflow()))));

        var request = updateRequestWithCognitoAdminAuthorization(updateCustomer, getIdentifierPathParam(updateCustomer));
        var response = sendUpdateRequest(request, CustomerDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getBodyObject(CustomerDto.class).getPublicationWorkflow(),
                   is(equalTo(updateCustomer.getPublicationWorkflow())));
    }

    @Test
    void shouldThrowForbiddenWhenTryingToUpdatePublicationWorkflowWhenNotAuthorized() throws IOException {
        createCustomerInLocalDb();
        var updateCustomer = changePublicationWorkflowFromPreExistingCustomer();
        var request = updateRequestWithoutCorrectAuthorization(updateCustomer, getIdentifierPathParam(updateCustomer));
        var response = sendUpdateRequest(request, CustomerDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    private Map<String, String> getIdentifierPathParam(CustomerDto customerDto) {
        return Map.of(IDENTIFIER, customerDto.getIdentifier().toString());
    }

    private CustomerDto changePublicationWorkflowFromPreExistingCustomer() {
        return preExistingCustomer.copy().withPublicationWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY).build();
    }

    private InputStream updateRequestWithCorrectAuthorization(CustomerDto updateCustomer,
                                                              Map<String, String> pathParameters)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper).withPathParameters(pathParameters)
                   .withCurrentCustomer(updateCustomer.getId())
                   .withAccessRights(updateCustomer.getId(),
                                     AccessRight.MANAGE_OWN_AFFILIATION)
                   .withBody(updateCustomer)
                   .build();
    }

    private InputStream updateRequestWithCognitoAdminAuthorization(CustomerDto updateCustomer,
                                                              Map<String, String> pathParameters)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper).withPathParameters(pathParameters)
                   .withCurrentCustomer(updateCustomer.getId())
                   .withAuthorizerClaim(SCOPE_CLAIM, AWS_COGNITO_SIGNIN_USER_ADMIN)
                   .withBody(updateCustomer)
                   .build();
    }

    private InputStream updateRequestWithoutCorrectAuthorization(CustomerDto updateCustomer,
                                                                 Map<String, String> pathParameters)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper).withPathParameters(pathParameters)
                   .withBody(updateCustomer)
                   .build();
    }

    private void createCustomerInLocalDb() throws IOException {
        var createCustomerBase = createCustomer(UUID.randomUUID());
        var createOutputStream = new ByteArrayOutputStream();
        createHandler.handleRequest(createCustomerInDbRequest(createCustomerBase), createOutputStream,
                                    new FakeContext());

        preExistingCustomer = GatewayResponse.fromOutputStream(createOutputStream, CustomerDto.class)
                                  .getBodyObject(CustomerDto.class);
    }

    private InputStream createCustomerInDbRequest(CustomerDto customer) throws JsonProcessingException {
        var authorizedCustomerId = randomUri();
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper).withBody(customer)
                   .withHeaders(getRequestHeaders())
                   .withCurrentCustomer(authorizedCustomerId)
                   .withAccessRights(authorizedCustomerId, AccessRight.MANAGE_CUSTOMERS)
                   .build();
    }

    private CustomerDto createCustomer(UUID uuid) {
        return CustomerDto.builder()
                   .withIdentifier(uuid)
                   .withName("New Customer")
                   .withCreatedDate(NOW)
                   .withModifiedDate(NOW)
                   .withPublicationWorkflow(REGISTRATOR_PUBLISHES_METADATA_AND_FILES)
                   .withCustomerOf(randomElement(ApplicationDomain.values()))
                   .build();
    }

    private <T> GatewayResponse<T> sendUpdateRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }
}
