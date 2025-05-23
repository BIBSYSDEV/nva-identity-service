package no.unit.nva.cognito;

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
import no.unit.nva.database.TermsAndConditionsService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.JwtTestToken;
import no.unit.nva.useraccessservice.dao.RoleDb;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserAttributesRequest;

import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_TERMS;
import static no.unit.nva.cognito.CognitoClaims.CUSTOMER_ACCEPTED_TERMS;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ROLES_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.cognito.CustomerSelectionHandler.AUTHORIZATION_HEADER;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.API_DOMAIN;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.CRISTIN_PATH;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerSelectionHandlerTest {

    public static final String MULTI_VALUE_CLAIMS_DELIMITER = ",";
    static final URI TERMS_URL = URI.create("https://nva.sikt.no/terms/2024-10-01");
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
    private AccessRight accessRight;
    private RoleDto role;
    private TermsAndConditionsService termsService;

    @BeforeEach
    public void init() throws InvalidInputException, ConflictException {
        outputStream = new ByteArrayOutputStream();
        setupCustomerService();
        setupIdentityService();
        accessRight = randomElement(AccessRight.values());
        role = randomRoleWithAccessRight(accessRight).toRoleDto();
        addRole(role);
        personAccessToken = JwtTestToken.randomToken();
        var person = randomUri();
        setupCognitoAndPersonInformation(person);
        termsService = mock(TermsAndConditionsService.class);
        var terms = TermsConditionsResponse.builder()
                        .withTermsConditionsUri(TERMS_URL)
                        .build();
        when(termsService.getCurrentTermsAndConditions()).thenReturn(terms);
        when(termsService.getTermsAndConditionsByPerson(any())).thenReturn(terms);

        addUserEntriesInIdentityService(person, allowedCustomers, role);

        this.handler = new CustomerSelectionHandler(cognito, customerService, identityService, termsService,
                                                    new Environment());
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
        var currentTerms = AttributeType.builder().name(CURRENT_TERMS).value(CURRENT_TERMS).build();
        var acceptedTerms =
            AttributeType.builder().name(CUSTOMER_ACCEPTED_TERMS).value(CUSTOMER_ACCEPTED_TERMS).build();

        cognito.updateUserAttributes(UpdateUserAttributesRequest.builder()
                                         .accessToken(personAccessToken)
                                         .userAttributes(currentTerms, acceptedTerms)
                                         .build());
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
                   .filter(attribute -> attribute.name().equals(PERSON_ID_CLAIM))
                   .map(AttributeType::value)
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow(fail -> new RuntimeException("Could not find " + PERSON_ID_CLAIM));
    }

    private GetUserRequest creteGetUserRequest() {
        return GetUserRequest.builder().accessToken(personAccessToken).build();
    }

    private void addRole(RoleDto role) throws InvalidInputException, ConflictException {
        identityService.addRole(role);
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

    private <T> GatewayResponse<T> sendRequest(InputStream input, Class<T> responseType) throws IOException {
        handler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
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

    private InputStream createRequest(URI customerId) throws JsonProcessingException {
        var randomCustomer = CustomerSelection.fromCustomerId(customerId);
        return new HandlerRequestBuilder<CustomerSelection>(dtoObjectMapper)
                   .withHeaders(Map.of(AUTHORIZATION_HEADER, bearerToken()))
                   .withPersonCristinId(randomUri())
                   .withCurrentCustomer(randomUri())
                   .withUserName(randomString())
                   .withBody(randomCustomer)
                   .build();
    }

    private String bearerToken() {
        return "Bearer " + personAccessToken;
    }

    @Test
    void shouldSendAnUpdateAccessRightRequestToCognitoWhenInputContainsAnAccessTokenAndSelectionIsAmongTheValidOptions()
        throws IOException {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        var response = sendRequest(input, Void.class);
        var updatedAccessRights = extractAttributeUpdate(ACCESS_RIGHTS_CLAIM);
        var expectedAccessRights = accessRight.toPersistedString();

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(updatedAccessRights, is(equalTo(expectedAccessRights)));
    }

    @Test
    void shouldUpdateRoleInCognitoUserEntry()
        throws IOException {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        var response = sendRequest(input, Void.class);
        var updatedRoles = extractAttributeUpdate(ROLES_CLAIM);
        var expectedRoles = role.getRoleName();
        assertThat(updatedRoles, is(equalTo(expectedRoles.getValue())));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
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

    private String constructExpectedUserName(URI selectedCustomer) throws NotFoundException {
        var personId = URI.create(extractPersonIdFromCognitoData());
        var customerCristinId = customerService.getCustomer(selectedCustomer).getCristinId();
        var user = identityService.getUserByPersonCristinIdAndCustomerCristinId(personId, customerCristinId);
        return user.getUsername();
    }

    @Test
    void shouldNotSendAnUpdateCustomerRequestToCognitoWhenInputDoesNotContainAccessToken()
        throws IOException {
        cognito.updateUserAttributes((UpdateUserAttributesRequest) null);//clear mock value@
        var input = createRequest(randomUri());
        var response = sendRequest(input, Void.class);
        assertThatUpdateRequestHasNotBeenSent();
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    private void assertThatUpdateRequestHasNotBeenSent() {
        assertThat(cognito.getUpdateUserAttributesRequest(), is(nullValue()));
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
    void shouldUpdateCustomerCristinWhenSelectingCustomer() throws IOException, NotFoundException {
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
}