package no.unit.nva.cognito;

import static no.unit.nva.cognito.CustomerSelectionHandler.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CustomerSelectionHandler.AT;
import static no.unit.nva.cognito.CustomerSelectionHandler.AUTHORIZATION_HEADER;
import static no.unit.nva.cognito.CustomerSelectionHandler.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CustomerSelectionHandler.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CustomerSelectionHandler.PERSON_ID_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.FakeCognito;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDtoWithoutContext;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.CustomerDynamoDBLocal;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

class CustomerSelectionHandlerTest extends CustomerDynamoDBLocal {

    public static final String MULTI_VALUE_CLAIMS_DELIMITER = ",";
    private final FakeContext context = new FakeContext();
    private FakeCognito cognito;
    private String accessToken;
    private Set<URI> allowedCustomers;
    private CustomerService customerService;
    private CustomerSelectionHandler handler;
    private String personIdentifier;

    @BeforeEach
    public void init() {
        super.setupDatabase();
        cognito = new FakeCognito(randomString());
        this.customerService = new DynamoDBCustomerService(dynamoClient);
        allowedCustomers = addCustomersToDatabase();
        personIdentifier = randomString();
        accessToken = randomString();
        var user = createUser(allowedCustomers, personIdentifier);
        cognito.addUser(accessToken, user);

        this.handler = new CustomerSelectionHandler(cognito, customerService);
    }

    private Set<URI> addCustomersToDatabase() {
        return IntStream.range(1, 10).boxed().map(ignored -> createRandomCustomer())
            .map(customer -> customerService.createCustomer(customer))
            .map(customer -> customerService.getCustomer(customer.getIdentifier()))
            .map(CustomerDtoWithoutContext::getId)
            .collect(Collectors.toSet());
    }

    private CustomerDto createRandomCustomer() {
        return CustomerDto.builder()
            .withIdentifier(UUID.randomUUID())
            .withCristinId(randomUri())
            .build();
    }

    @Test
    void shouldSendAnUpdateCustomerRequestToCognitoWhenInputContainsAnAccessTokenAndSelectionIsAmongTheValidOptions() {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        var response = handler.handleRequest(input, context);
        var updatedSelectedCustomer = extractAttributeUpdate(CURRENT_CUSTOMER_CLAIM);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(URI.create(updatedSelectedCustomer), is(in(allowedCustomers.toArray(URI[]::new))));
    }

    @Test
    void shouldUpdateNvaUsernameInCognitoUserEntry() {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        var response = handler.handleRequest(input, context);
        var userForSelectedCustomer = extractAttributeUpdate(NVA_USERNAME_CLAIM);
        var expectedUsername = constructExpectedUserName(selectedCustomer);
        assertThat(userForSelectedCustomer, is(equalTo(expectedUsername)));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    private String constructExpectedUserName(URI selectedCustomer) {
        var personIdentifier = UriWrapper.fromUri(extractPreExistingAttribute(PERSON_ID_CLAIM))
            .getLastPathElement();
        var customerCristinIdentifier = attempt(() -> customerService.getCustomer(selectedCustomer))
            .map(CustomerDto::getCristinId)
            .map(UriWrapper::fromUri)
            .map(UriWrapper::getLastPathElement)
            .orElseThrow();

        return personIdentifier + AT + customerCristinIdentifier;
    }

    private String extractPreExistingAttribute(String attributeName) {
        return cognito.getUser(GetUserRequest.builder().accessToken(accessToken).build())
            .userAttributes()
            .stream()
            .filter(attribute -> attribute.name().equals(attributeName))
            .map(AttributeType::value)
            .collect(SingletonCollector.tryCollect())
            .orElseThrow(fail -> new RuntimeException("Could not find " + attributeName));
    }

    private String extractAttributeUpdate(String attributeName) {
        var request = cognito.getUpdateUserAttributesRequest();
        return request
            .userAttributes()
            .stream()
            .filter(attribute -> attributeName.equals(attribute.name()))
            .map(AttributeType::value)
            .collect(SingletonCollector.collect());
    }

    @Test
    void shouldNotSendAnUpdateCustomerRequestToCognitoWhenInputContainsAnAccessTokenAndSelectionIsAmongTheValidOptions() {

        var input = createRequest(randomUri());
        var response = handler.handleRequest(input, context);
        assertThatUpdateRequestHasNotBeenSent();
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldNotUserAdminRightsButOnlyAccessTokenBasedAccessToCognito() {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        handler.handleRequest(input, context);
        assertThat(cognito.getAdminUpdateUserRequest(), is(nullValue()));
        assertThat(cognito.getUpdateUserAttributesRequest(), is(not(nullValue())));
    }

    private GetUserResponse createUser(Set<URI> allowedCustomers, String personIdentifier) {
        var allowedCustomersString = allowedCustomers.stream()
            .map(URI::toString)
            .collect(Collectors.joining(MULTI_VALUE_CLAIMS_DELIMITER));
        var allowedCustomersClaim = createAttribute(ALLOWED_CUSTOMERS_CLAIM, allowedCustomersString);
        var cristinPersonIdClaim = createAttribute(PERSON_ID_CLAIM, personIdentifier);
        return GetUserResponse.builder()
            .userAttributes(allowedCustomersClaim, cristinPersonIdClaim)
            .build();
    }

    private AttributeType createAttribute(String attributeName, String attributeValue) {
        return AttributeType.builder()
            .name(attributeName)
            .value(attributeValue)
            .build();
    }

    private void assertThatUpdateRequestHasNotBeenSent() {
        assertThat(cognito.getUpdateUserAttributesRequest(), is(nullValue()));
    }

    private APIGatewayProxyRequestEvent createRequest(URI customerId) {
        String randomCustomer = CustomerSelection.fromCustomerId(customerId).toString();
        return new APIGatewayProxyRequestEvent()
            .withHeaders(Map.of(AUTHORIZATION_HEADER, bearerToken()))
            .withBody(randomCustomer);
    }

    private String bearerToken() {
        return "Bearer " + accessToken;
    }
}