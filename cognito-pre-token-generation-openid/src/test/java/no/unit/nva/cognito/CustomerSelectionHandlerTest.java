package no.unit.nva.cognito;

import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.cognito.CustomerSelectionHandler.AUTHORIZATION_HEADER;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.FakeCognito;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

class CustomerSelectionHandlerTest {

    public static final String MULTI_VALUE_CLAIMS_DELIMITER = ",";
    private final FakeContext context = new FakeContext();
    private FakeCognito cognito;
    private String personAccessToken;
    private Set<URI> allowedCustomers;
    private CustomerService customerService;
    private CustomerSelectionHandler handler;
    private IdentityService identityService;
    private LocalCustomerServiceDatabase customerDatabase;
    private LocalIdentityService usersDatabase;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        outputStream = new ByteArrayOutputStream();
        setupCustomerService();
        setupIdentityService();
        var person = randomUri();
        personAccessToken = randomString();
        setupCognitoAndPersonInformation(person);

        addUserEntriesInIdentityService(person, allowedCustomers);

        this.handler = new CustomerSelectionHandler(cognito, customerService, identityService);
    }

    @AfterEach
    public void close() {
        customerDatabase.deleteDatabase();
        usersDatabase.closeDB();
    }

    @Test
    void shouldSendAnUpdateCustomerRequestToCognitoWhenInputContainsAnAccessTokenAndSelectionIsAmongTheValidOptions()
        throws IOException {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        var response = sendRequest(input, Void.class);
        var updatedSelectedCustomer = extractAttributeUpdate(CURRENT_CUSTOMER_CLAIM);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(URI.create(updatedSelectedCustomer), is(in(allowedCustomers.toArray(URI[]::new))));
    }

    @Test
    void shouldUpdateNvaUsernameInCognitoUserEntry() throws IOException, NotFoundException {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        var response = sendRequest(input, Void.class);
        var userForSelectedCustomer = extractAttributeUpdate(NVA_USERNAME_CLAIM);
        var expectedUsername = constructExpectedUserName(selectedCustomer);
        assertThat(userForSelectedCustomer, is(equalTo(expectedUsername)));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldNotSendAnUpdateCustomerRequestToCognitoWhenInputDoesNotContainAccessToken()
        throws IOException {
        var input = createRequest(randomUri());
        var response = sendRequest(input, Void.class);
        assertThatUpdateRequestHasNotBeenSent();
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldNotUseAdminRightsButOnlyAccessTokenBasedAccessToCognito() throws IOException {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        sendRequest(input, Void.class);
        assertThat(cognito.getAdminUpdateUserRequest(), is(nullValue()));
        assertThat(cognito.getUpdateUserAttributesRequest(), is(not(nullValue())));
    }

    @Test
    void shouldUpdateCustomerCristinIfWhenSelectingCustomer() throws IOException, NotFoundException {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        sendRequest(input, Void.class);
        var cristinIdForSelectedCustomer = extractAttributeUpdate(TOP_ORG_CRISTIN_ID);
        var expectedCristinId = customerService.getCustomer(selectedCustomer).getCristinId();
        assertThat(cristinIdForSelectedCustomer, is(equalTo(expectedCristinId.toString())));
    }

    @Test
    void shouldUpdatePersonAffiliationWhenSelectingCustomer() throws IOException, NotFoundException {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        handler.handleRequest(input, outputStream, context);
        var cristinPersonId = URI.create(extractPersonIdFromCognitoData());
        var customerCristinId = customerService.getCustomer(selectedCustomer).getCristinId();
        var user = identityService.getUserByPersonCristinIdAndCustomerCristinId(cristinPersonId, customerCristinId);
        var expectedPersonAffiliation = user.getAffiliation();
        var actualPersonAffiliation = extractAttributeUpdate(PERSON_AFFILIATION_CLAIM);
        assertThat(actualPersonAffiliation, is(equalTo(expectedPersonAffiliation.toString())));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream input, Class<T> responseType) throws IOException {
        handler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private void setupCognitoAndPersonInformation(URI person) {
        cognito = new FakeCognito(randomString());
        var user = createUserEntryInCognito(allowedCustomers, person);
        cognito.addUser(personAccessToken, user);
    }

    private void setupIdentityService() {
        usersDatabase = new LocalIdentityService();
        usersDatabase.initializeTestDatabase();
        this.identityService = new IdentityServiceImpl(usersDatabase.getDynamoDbClient());
    }

    private void setupCustomerService() {
        customerDatabase = new LocalCustomerServiceDatabase();
        customerDatabase.setupDatabase();
        this.customerService = new DynamoDBCustomerService(customerDatabase.getDynamoClient());
        allowedCustomers = addCustomersToDatabase();
    }

    private void addUserEntriesInIdentityService(URI personId, Set<URI> allowedCustomers) {
        allowedCustomers.stream()
            .map(customerId -> createUserEntryInIdentityService(customerId, personId))
            .forEach(user -> addUser(user));
    }

    private UserDto addUser(UserDto user) {
        return attempt(() -> identityService.addUser(user)).orElseThrow();
    }

    private UserDto createUserEntryInIdentityService(URI selectedCustomer, URI personId) {
        var customer = attempt(() -> customerService.getCustomer(selectedCustomer)).orElseThrow();
        var cristinPersonId = URI.create(extractPersonIdFromCognitoData());
        var nvaUsername = constructUserName();

        return UserDto.newBuilder()
            .withUsername(nvaUsername)
            .withCristinId(personId)
            .withInstitution(customer.getId())
            .withCristinId(cristinPersonId)
            .withInstitutionCristinId(customer.getCristinId())
            .withAffiliation(randomUri())
            .build();
    }

    private String constructUserName() {
        return randomString();
    }

    private Set<URI> addCustomersToDatabase() {
        return IntStream.range(1, 10).boxed().map(ignored -> createRandomCustomer())
            .map(attempt(customer -> customerService.createCustomer(customer)))
            .map(attempt -> attempt.map(customer -> customerService.getCustomer(customer.getIdentifier())))
            .map(Try::orElseThrow)
            .map(CustomerDto::getId)
            .collect(Collectors.toSet());
    }

    private CustomerDto createRandomCustomer() {
        return CustomerDto.builder()
            .withIdentifier(UUID.randomUUID())
            .withCristinId(randomUri())
            .build();
    }

    private String constructExpectedUserName(URI selectedCustomer) throws NotFoundException {
        var personId = URI.create(extractPersonIdFromCognitoData());
        var customerCristinId = customerService.getCustomer(selectedCustomer).getCristinId();
        var user = identityService.getUserByPersonCristinIdAndCustomerCristinId(personId, customerCristinId);
        return user.getUsername();
    }

    private String extractPersonIdFromCognitoData() {
        return cognito.getUser(creteGetUserRequest())
            .userAttributes()
            .stream()
            .filter(attribute -> attribute.name().equals(CognitoClaims.PERSON_ID_CLAIM))
            .map(AttributeType::value)
            .collect(SingletonCollector.tryCollect())
            .orElseThrow(fail -> new RuntimeException("Could not find " + CognitoClaims.PERSON_ID_CLAIM));
    }

    private GetUserRequest creteGetUserRequest() {
        return GetUserRequest.builder().accessToken(personAccessToken).build();
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

    private GetUserResponse createUserEntryInCognito(Set<URI> allowedCustomers, URI personId) {
        var allowedCustomersString = allowedCustomers.stream()
            .map(URI::toString)
            .collect(Collectors.joining(MULTI_VALUE_CLAIMS_DELIMITER));
        var allowedCustomersClaim = createAttribute(ALLOWED_CUSTOMERS_CLAIM, allowedCustomersString);
        var cristinPersonIdClaim = createAttribute(PERSON_ID_CLAIM, personId.toString());
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

    private InputStream createRequest(URI customerId) throws JsonProcessingException {
        var randomCustomer = CustomerSelection.fromCustomerId(customerId);
        return new HandlerRequestBuilder<CustomerSelection>(dtoObjectMapper)
            .withHeaders(Map.of(AUTHORIZATION_HEADER, bearerToken()))
            .withBody(randomCustomer)
            .build();
    }

    private String bearerToken() {
        return "Bearer " + personAccessToken;
    }
}