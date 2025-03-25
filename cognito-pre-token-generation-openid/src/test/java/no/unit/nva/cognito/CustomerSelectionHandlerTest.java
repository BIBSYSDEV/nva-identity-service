package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.FakeCognito;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.JwtTestToken;
import no.unit.nva.useraccessservice.dao.RoleDb;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.auth.CognitoUserInfo.COGNITO_USER_NAME;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.API_DOMAIN;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.CRISTIN_PATH;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;

class CustomerSelectionHandlerTest {

    public static final String MULTI_VALUE_CLAIMS_DELIMITER = ",";
    private final Context context = mock(Context.class);
    private FakeCognito cognito;
    private String personAccessToken;
    private Set<URI> allowedCustomers;
    private CustomerService customerService;
    private CustomerSelectionHandler handler;
    private IdentityService identityService;
    private LocalCustomerServiceDatabase customerDatabase;
    private LocalIdentityService usersDatabase;
    private ByteArrayOutputStream outputStream;
    private AccessRight accessRight;
    private RoleDto role;

    @BeforeEach
    void init() throws InvalidInputException, ConflictException {
        outputStream = new ByteArrayOutputStream();
        setupCustomerService();
        setupIdentityService();
        accessRight = randomElement(AccessRight.values());
        role = randomRoleWithAccessRight(accessRight).toRoleDto();
        addRole(role);
        personAccessToken = JwtTestToken.randomToken();
        var person = randomUri();
        setupCognitoAndPersonInformation(person);

        addUserEntriesInIdentityService(person, allowedCustomers, role);

        this.handler = new CustomerSelectionHandler(cognito);
    }

    private static RoleDb randomRoleWithAccessRight(AccessRight accessRight) {
        return RoleDb.newBuilder()
                   .withName(randomRoleName())
                   .withAccessRights(Set.of(accessRight))
                   .build();
    }

    private void setupCognitoAndPersonInformation(URI person) {
        cognito = new FakeCognito(randomString());
        var user = createUserEntryInCognito(allowedCustomers, person);
        cognito.addUser(personAccessToken, user);
    }

    private GetUserResponse createUserEntryInCognito(Set<URI> allowedCustomers, URI personId) {
        var allowedCustomersString = allowedCustomers.stream()
                                         .map(URI::toString)
                                         .collect(Collectors.joining(MULTI_VALUE_CLAIMS_DELIMITER));
        var allowedCustomersClaim = createAttribute(ALLOWED_CUSTOMERS_CLAIM, allowedCustomersString);
        var cristinPersonIdClaim = createAttribute(PERSON_CRISTIN_ID_CLAIM, personId.toString());
        return GetUserResponse.builder()
                   .userAttributes(allowedCustomersClaim, cristinPersonIdClaim)
                   .username(randomString())
                   .build();
    }

    private AttributeType createAttribute(String attributeName, String attributeValue) {
        return AttributeType.builder()
                   .name(attributeName)
                   .value(attributeValue)
                   .build();
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
                   .withCristinId(randomOrgUri())
                   .withCustomerOf(randomElement(ApplicationDomain.values()))
                   .build();
    }

    private URI randomOrgUri() {
        return UriWrapper.fromHost(API_DOMAIN).addChild(CRISTIN_PATH).addChild(randomString()).getUri();
    }

    private void addUserEntriesInIdentityService(URI personId, Set<URI> allowedCustomers, RoleDto role) {
        allowedCustomers.stream()
            .map(customerId -> createUserEntryInIdentityService(customerId, personId, role))
            .forEach(this::addUser);
    }

    private void addUser(UserDto user) {
        attempt(() -> identityService.addUser(user)).orElseThrow();
    }

    private UserDto createUserEntryInIdentityService(URI selectedCustomer, URI personId, RoleDto role) {
        var customer = attempt(() -> customerService.getCustomer(selectedCustomer)).orElseThrow();
        var cristinPersonId = URI.create(extractPersonIdFromCognitoData());
        var nvaUsername = constructUserName();

        return UserDto.newBuilder()
                   .withUsername(nvaUsername)
                   .withCristinId(personId)
                   .withInstitution(customer.getId())
                   .withCristinId(cristinPersonId)
                   .withInstitutionCristinId(customer.getCristinId())
                   .withRoles(Set.of(role))
                   .withAffiliation(randomUri())
                   .build();
    }

    private String constructUserName() {
        return randomString();
    }

    private String extractPersonIdFromCognitoData() {
        return cognito.getUser(creteGetUserRequest())
                   .userAttributes()
                   .stream()
                   .filter(attribute -> attribute.name().equals(PERSON_CRISTIN_ID_CLAIM))
                   .map(AttributeType::value)
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow(fail -> new RuntimeException("Could not find " + PERSON_CRISTIN_ID_CLAIM));
    }

    private GetUserRequest creteGetUserRequest() {
        return GetUserRequest.builder().accessToken(personAccessToken).build();
    }

    private void addRole(RoleDto role) throws InvalidInputException, ConflictException {
        identityService.addRole(role);
    }

    @AfterEach
    void close() {
        customerDatabase.deleteDatabase();
        usersDatabase.closeDB();
    }

    @Test
    void shouldSendAnUpdateCustomerRequestToCognitoWhenInputContainsAnAccessTokenAndSelectionIsAmongTheValidOptions()
        throws IOException {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        var response = sendRequest(input, Void.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        var updatedSelectedCustomer = extractSelectedCustomerAttributeUpdate();

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(updatedSelectedCustomer, is(in(allowedCustomers.toArray(URI[]::new))));
    }

    @Test
    void shouldDeleteAllUserAttributesOnCustomerSelect()
        throws IOException {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        var response = sendRequest(input, Void.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        assertThat(cognito.getAdminDeleteUserAttributesRequest(), is(notNullValue()));
    }

    @Test
    void shouldDenyRequestWhenCustomerSelectionIsNotAmongTheValidOptions() throws IOException {
        var invalidCustomer = randomUri();
        var input = createRequest(invalidCustomer);
        var response = sendRequest(input, Void.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream input, Class<T> responseType) throws IOException {
        handler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private URI extractSelectedCustomerAttributeUpdate() {

        var request = cognito.getAdminUpdateUserRequest();
        return request
                   .userAttributes()
                   .stream()
                   .filter(attribute -> CURRENT_CUSTOMER_CLAIM.equals(attribute.name()))
                   .map(AttributeType::value)
                   .map(URI::create).findFirst().orElseThrow();
    }

    private InputStream createRequest(URI customerId) throws JsonProcessingException {
        var randomCustomer = CustomerSelection.fromCustomerId(customerId);

        String testCognitoGroupId = "test_cognito_group_id";
        return new HandlerRequestBuilder<CustomerSelection>(dtoObjectMapper)
                   .withPersonCristinId(randomUri())
                   .withCurrentCustomer(customerId)
                   .withAllowedCustomers(allowedCustomers)
                   .withUserName(randomString())
                   .withIssuer(randomUri() + "/" + testCognitoGroupId)
                   .withAuthorizerClaim(COGNITO_USER_NAME,
                                        cognito.getUser(GetUserRequest.builder().accessToken(personAccessToken).build())
                                            .username())
                   .withBody(randomCustomer)
                   .build();
    }
}