package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.handlers.models.CreateUserRequest;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.ViewingScope;
import no.unit.nva.useraccessservice.userceation.testing.cristin.AuthenticationScenarios;
import no.unit.nva.useraccessservice.userceation.testing.cristin.MockPersonRegistry;
import no.unit.nva.useraccessservice.userceation.testing.cristin.MockedPersonData;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfoConstants;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.unit.nva.RandomUserDataGenerator.randomRoleNameButNot;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.API_DOMAIN;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_NAME;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_VALUE;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_AFFILIATION;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WireMockTest(httpsEnabled = true)
class CreateUserHandlerTest extends HandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserHandlerTest.class);
    private final FakeSecretsManagerClient secretsManagerClient = new FakeSecretsManagerClient();
    private CreateUserHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;
    private LocalIdentityService identityServiceDb;
    private IdentityService identityService;
    private LocalCustomerServiceDatabase customerServiceDatabase;
    private CustomerService customerService;
    private AuthenticationScenarios scenarios;
    private PersonRegistry personRegistry;

    @BeforeEach
    public void init(WireMockRuntimeInfo wireMockRuntimeInfo) throws InvalidInputException, ConflictException {
        this.customerService = initializeCustomerService();
        this.identityService = initializeIdentityService();

        var cristinUsername = randomString();
        var cristinPassword = randomString();
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_USERNAME_SECRET_KEY, cristinUsername);
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_PASSWORD_SECRET_KEY, cristinPassword);

        var wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());

        var defaultRequestHeaders = new HttpHeaders()
            .withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);
        var mockPersonRegistry = new MockPersonRegistry(cristinUsername,
            cristinPassword,
            wiremockUri,
            defaultRequestHeaders);

        this.scenarios = new AuthenticationScenarios(mockPersonRegistry, customerService, identityService);

        var httpClient = WiremockHttpClient.create();
        personRegistry = CristinPersonRegistry.customPersonRegistry(
            httpClient,
            wiremockUri,
            ServiceConstants.API_DOMAIN,
            defaultRequestHeaders,
            new SecretsReader(secretsManagerClient));

        handler = new CreateUserHandler(identityService, customerService, personRegistry,
                                        new Environment());
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    private DynamoDBCustomerService initializeCustomerService() {
        customerServiceDatabase = new LocalCustomerServiceDatabase();
        customerServiceDatabase.setupDatabase();
        return new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());
    }

    private IdentityService initializeIdentityService() {
        this.identityServiceDb = new LocalIdentityService();
        var client = identityServiceDb.initializeTestDatabase();
        return IdentityService.defaultIdentityService(client);
    }

    @AfterEach
    public void close() {
        identityServiceDb.closeDB();
        customerServiceDatabase.deleteDatabase();
    }

    @Test
    void shouldCreateUserWithRequestedRolesAndViewingScopeWhenCreatedWithNin()
        throws IOException {

        var person = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();

        var someCustomer = fetchSomeCustomerForThePerson(person);
        var providedViewingScope = ViewingScope.defaultViewingScope(randomCristinOrganization());
        var requestBody = sampleRequestForExistingPersonCustomerAndRolesWithoutCristinId(person, someCustomer.getId(),
            providedViewingScope);
        var request = createRequest(requestBody, someCustomer, MANAGE_OWN_AFFILIATION);
        var response = sendRequest(request, UserDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));

        var actualUser = identityService.listUsers(requestBody.customerId())
            .stream().collect(SingletonCollector.collect());

        var expectedRoles = createExpectedRoleSet(requestBody);
        var persistedViewingScope = actualUser.getViewingScope();

        assertThat(persistedViewingScope, is(equalTo(providedViewingScope)));
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.customerId())));
        assertThat(actualUser.getRoles(), is(equalTo(expectedRoles)));
    }

    private URI randomCristinOrganization() {
        return UriWrapper.fromHost(API_DOMAIN)
            .addChild("cristin")
            .addChild("organization")
            .addChild(randomString())
            .getUri();
    }

    private Set<RoleDto> createExpectedRoleSet(CreateUserRequest requestBody) {
        return Stream.of(requestBody.roles(),
                Set.of(UserEntriesCreatorForPerson.ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private CreateUserRequest sampleRequestForExistingPersonCustomerAndRolesWithoutCristinId(MockedPersonData person,
                                                                                             URI customerId,
                                                                                             ViewingScope viewingScope) {
        return new CreateUserRequest(person.nin(), null, customerId, randomRoles(), viewingScope);
    }

    private Set<RoleDto> randomRoles() {
        var role = RoleDto.newBuilder().withRoleName(randomRoleNameButNot(RoleName.APPLICATION_ADMIN)).build();
        addRoleToIdentityServiceBecauseNonExistingRolesAreIgnored(role);
        return Set.of(role);
    }

    private void addRoleToIdentityServiceBecauseNonExistingRolesAreIgnored(RoleDto role) {
        var existingRole = attempt(() -> identityService.getRole(role)).toOptional();
        try {
            if (existingRole.isEmpty()) {
                identityService.addRole(role);
            }
        } catch (ConflictException | InvalidInputException e) {
            throw new RuntimeException(e);
        }
    }

    private CustomerDto fetchSomeCustomerForThePerson(MockedPersonData person) {
        var cristinIds = scenarios.getCristinUriForInstitutionAffiliations(person.nin(), true);

        LOGGER.info("Cristin ids: {}", cristinIds);

        var customer = cristinIds.stream()
            .map(attempt(institution -> customerService.getCustomerByCristinId(institution)))
            .map(Try::orElseThrow)
            .findFirst()
            .orElseThrow();

        LOGGER.info("Using customer {} with cristin id {}", customer.getId(), customer.getCristinId());
        return customer;
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream createRequest(CreateUserRequest requestBody,
                                      CustomerDto customer,
                                      AccessRight... accessRights)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
            .withCurrentCustomer(customer.getId())
            .withAccessRights(customer.getId(), accessRights)
            .withBody(requestBody)
            .build();
    }

    @Test
    void shouldCreateUserWithRequestedRolesAndViewingScopeWhenCreatedWithCristinId()
        throws IOException {

        var person = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();

        var someCustomer = fetchSomeCustomerForThePerson(person);
        var providedViewingScope = ViewingScope.defaultViewingScope(randomCristinOrganization());
        var requestBody = sampleRequestForExistingPersonCustomerAndRolesWithoutNin(person, someCustomer.getId(),
            providedViewingScope);
        var request = createRequest(requestBody, someCustomer, MANAGE_OWN_AFFILIATION);
        var response = sendRequest(request, UserDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));

        var actualUser = identityService.listUsers(requestBody.customerId())
            .stream().collect(SingletonCollector.collect());

        var expectedRoles = createExpectedRoleSet(requestBody);
        var persistedViewingScope = actualUser.getViewingScope();

        assertThat(persistedViewingScope, is(equalTo(providedViewingScope)));
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.customerId())));
        assertThat(actualUser.getRoles(), is(equalTo(expectedRoles)));
    }

    private CreateUserRequest sampleRequestForExistingPersonCustomerAndRolesWithoutNin(MockedPersonData person,
                                                                                       URI customerId,
                                                                                       ViewingScope viewingScope) {
        return new CreateUserRequest(null, person.cristinIdentifier(), customerId, randomRoles(), viewingScope);
    }

    @Test
    void shouldCreateUserWithRequestedRolesWhenInputContainsNationalIdNumberInstitutionIdAndSetOfRoles()
        throws IOException {

        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = fetchSomeCustomerForThePerson(person);

        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getCristinId(),
            ViewingScope.defaultViewingScope(
                randomCristinOrganization()));

        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        var response = sendRequest(request, UserDto.class);

        var actualUser = identityService.listUsers(customer.getId())
            .stream().collect(SingletonCollector.collect());

        var expectedRoles = createExpectedRoleSet(requestBody);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        assertThat(actualUser.getInstitution(), is(equalTo(customer.getId())));
        assertThat(actualUser.getRoles(), is(equalTo(expectedRoles)));
    }

    private CreateUserRequest sampleRequestForExistingPersonCustomerAndRoles(MockedPersonData person,
                                                                             URI customerId,
                                                                             ViewingScope viewingScope) {
        return new CreateUserRequest(person.nin(), person.cristinIdentifier(), customerId, randomRoles(), viewingScope);
    }

    @Test
    void shouldReturnOkAndReturnExistingUserWithStatusCode200WhenTryingToCreateExistingUser() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = fetchSomeCustomerForThePerson(person);
        var organizationId = randomCristinOrganization();
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(
                organizationId));

        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        sendRequest(request, UserDto.class);

        outputStream = new ByteArrayOutputStream();
        request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        var response = sendRequest(request, UserDto.class);
        System.out.println(response.getBody());
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldDenyAccessToUsersThatAreDoNotHaveTheRightToCreateUsers() throws IOException {
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(randomPerson(), randomUri(),
            ViewingScope.defaultViewingScope(randomCristinOrganization()));
        var request = createRequestWithoutAccessRights(requestBody);
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    private MockedPersonData randomPerson() {
        return new MockedPersonData(randomString(), randomString());
    }

    private InputStream createRequestWithoutAccessRights(CreateUserRequest requestBody) throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
            .withCurrentCustomer(customerId)
            .withBody(requestBody)
            .build();
    }

    @Test
    void shouldDenyAccessWhenInstitutionAdminTriesToCreateAnAppAdmin() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody =
            new CreateUserRequest(person.nin(), person.cristinIdentifier(), customer.getId(), appAdminRole(),
                ViewingScope.defaultViewingScope(randomCristinOrganization()));

        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    private Set<RoleDto> appAdminRole() {
        var role = RoleDto.newBuilder().withRoleName(RoleName.APPLICATION_ADMIN).build();
        addRoleToIdentityServiceBecauseNonExistingRolesAreIgnored(role);
        return Set.of(role);
    }

    @Test
    void shouldAllowAccessWhenAppAdminTriesToCreateAnAppAdmin() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody =
            new CreateUserRequest(person.nin(), person.cristinIdentifier(), customer.getId(), appAdminRole(),
                ViewingScope.defaultViewingScope(randomCristinOrganization()));

        var request = createRequest(requestBody, customer, MANAGE_CUSTOMERS);
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    }

    @Test
    void shouldDenyAccessToUsersThatDoNotHaveTheRightToAddAnyUserAndTheyAreTryingToAddUsersForAnotherInstitution()
        throws IOException {
        var customerIdentifier = UUID.randomUUID();
        var customerId = UriWrapper.fromUri(randomUri()).addChild(customerIdentifier.toString()).getUri();
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(randomPerson(), customerId,
            ViewingScope.defaultViewingScope(randomCristinOrganization()));
        var customer = CustomerDto.builder().withIdentifier(customerIdentifier).build();
        var request = createRequest(requestBody, customer);

        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldAllowAccessToCustomerManagers() throws IOException {
        var person = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(
                randomCristinOrganization()));
        var request = createRequest(requestBody, customer, MANAGE_CUSTOMERS);

        var response = sendRequest(request, UserDto.class);

        var actualUser = response.getBodyObject(UserDto.class);
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.customerId())));
    }

    @Test
    void shouldAllowAccessToBackendServices()
        throws IOException {
        var person = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(
                randomCristinOrganization()));
        var request = createBackendRequest(requestBody);

        var response = sendRequest(request, UserDto.class);

        var actualUser = response.getBodyObject(UserDto.class);
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.customerId())));
    }

    private InputStream createBackendRequest(CreateUserRequest requestBody)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
            .withScope(RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE)
            .withBody(requestBody)
            .build();
    }

    @Test
    void shouldNotOverwriteRolesOfExistingUsers() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(randomCristinOrganization()));

        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        var response = sendRequest(request, UserDto.class);
        final var firstRoles = response.getBodyObject(UserDto.class).getRoles();

        outputStream = new ByteArrayOutputStream();
        //sending a create user request for the same user but with different roles
        requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(randomCristinOrganization()));
        request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        response = sendRequest(request, UserDto.class);
        var allRoles = response.getBodyObject(UserDto.class).getRoles();

        assertThat(allRoles, hasItems(firstRoles.toArray(RoleDto[]::new)));
    }

    @Test
    void shouldReturnExistingUserAndNotOverwriteItWhenAttemptingToCreateAlreadyExistingUser() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(randomCristinOrganization()));

        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        var response = sendRequest(request, UserDto.class);
        final var firstRoles = response.getBodyObject(UserDto.class).getRoles();

        outputStream = new ByteArrayOutputStream();
        //sending a create user request for the same user but with different includedUnits
        requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(randomCristinOrganization()));
        request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        response = sendRequest(request, UserDto.class);
        var allRoles = response.getBodyObject(UserDto.class).getRoles();

        assertThat(allRoles, hasItems(firstRoles.toArray(RoleDto[]::new)));
    }

    @Test
    void shouldReturnConflictErrorWhenPersonHasNoActiveAffiliations() throws IOException {
        var person = scenarios.personWithExactlyOneInactiveEmployment();
        var customer = randomCustomer();
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(customer.getCristinId()));
        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);

        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));

        var message = response.getBodyObject(Problem.class).getDetail();
        assertThat(message, matchesPattern(".*Person (.*) has no active affiliations with an institution.*"));
    }

    private CustomerDto randomCustomer() {
        return CustomerDto.builder()
            .withIdentifier(UUID.randomUUID())
            .withCristinId(randomCristinOrganization())
            .build();
    }

    @Test
    void shouldReturnConflictErrorWhenPersonHasActiveAffiliationsButNotWithAnExistingCustomer() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmploymentInNonCustomer();
        var customer = randomCustomer();
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(randomCristinOrganization()));
        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);

        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));

        var message = response.getBodyObject(Problem.class).getDetail();
        assertThat(message,
            matchesPattern(".*Person (.*) has no active affiliations with an Institution that"
                + " is an NVA customer.*"));
    }

    @Test
    void shouldReturnConflictErrorWhenPersonIsNotRegisteredInPersonRegistry() throws IOException {
        var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
        var customer = randomCustomer();
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(randomCristinOrganization()));
        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);

        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));

        var message = response.getBodyObject(Problem.class).getDetail();
        assertThat(message,
            matchesPattern(".*Person (.*) is not registered in the Person Registry.*"));
    }

    @Test
    void shouldReturnBadGatewayWhenSomethingUnexpectedError() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = randomCustomer();
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
            ViewingScope.defaultViewingScope(randomCristinOrganization()));

        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        var mockedIdentityService = mock(IdentityService.class);
        handler = new CreateUserHandler(mockedIdentityService, customerService, personRegistry,
                                        new Environment());
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));

        var message = response.getBodyObject(Problem.class).getDetail();
        assertThat(message,
            is(equalTo("Something went wrong, contact application administrator!")));
    }

    @Test
    void shouldReturnBadGatewayWhenFetchingPersonFromPersonRegistryFails() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = randomCustomer();
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
                ViewingScope.defaultViewingScope(randomCristinOrganization()));

        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        var mockedIdentityService = mock(IdentityService.class);
        var personRegistry = mock(PersonRegistry.class);
        when(personRegistry.fetchPersonByIdentifier(any())).thenThrow(RuntimeException.class);
        handler = new CreateUserHandler(mockedIdentityService, customerService, personRegistry,
                new Environment());
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));

        var message = response.getBodyObject(Problem.class).getDetail();
        assertThat(message,
                is(equalTo("Something went wrong, contact application administrator!")));
    }

    @Test
    void shouldReturnBadGatewayWhenFetchingPersonFromIdentityServiceFails() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = randomCustomer();
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId(),
                ViewingScope.defaultViewingScope(randomCristinOrganization()));

        var request = createRequest(requestBody, customer, MANAGE_OWN_AFFILIATION);
        var mockedIdentityService = mock(IdentityService.class);
        when(mockedIdentityService.getUsersByCristinId(any())).thenThrow(RuntimeException.class);
        handler = new CreateUserHandler(mockedIdentityService, customerService, personRegistry,
                new Environment());
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));

        var message = response.getBodyObject(Problem.class).getDetail();
        assertThat(message,
                is(equalTo("Something went wrong, contact application administrator!")));
    }
}
