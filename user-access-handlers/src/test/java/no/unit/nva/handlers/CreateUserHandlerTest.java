package no.unit.nva.handlers;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomAccessRight;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_NAME;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_VALUE;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
import static nva.commons.apigateway.AccessRight.ADMINISTRATE_APPLICATION;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_USERS;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.userceation.testing.cristin.AuthenticationScenarios;
import no.unit.nva.useraccessservice.userceation.testing.cristin.MockPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfoConstants;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;

@WireMockTest(httpsEnabled = true)
class CreateUserHandlerTest extends HandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserHandlerTest.class);

    private CreateUserHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;
    private LocalIdentityService identityServiceDb;
    private IdentityService identityService;
    private LocalCustomerServiceDatabase customerServiceDatabase;
    private CustomerService customerService;
    private AuthenticationScenarios scenarios;
    private final FakeSecretsManagerClient secretsManagerClient = new FakeSecretsManagerClient();

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

        var userCreator = new UserEntriesCreatorForPerson(identityService);

        var httpClient = WiremockHttpClient.create();
        var personRegistry = CristinPersonRegistry.customPersonRegistry(
            httpClient,
            wiremockUri,
            ServiceConstants.API_DOMAIN,
            defaultRequestHeaders,
            new SecretsReader(secretsManagerClient));

        handler = new CreateUserHandler(userCreator, identityService, customerService, personRegistry);
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    public void close() {
        identityServiceDb.closeDB();
        customerServiceDatabase.deleteDatabase();
    }

    @Test
    void shouldCreateUserWithRequestedRolesWhenInputContainsNationalIdNumberNvaCustomerIdAndSetOfRoles()
        throws IOException {

        String personNin = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();

        var someCustomer = fetchSomeCustomerForThePerson(personNin);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(personNin, someCustomer.getId());

        var request = createRequest(requestBody, someCustomer, EDIT_OWN_INSTITUTION_USERS);
        var response = sendRequest(request, UserDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        var actualUser = identityService.listUsers(requestBody.getCustomerId())
                             .stream().collect(SingletonCollector.collect());

        var expectedRoles = createExpectedRoleSet(requestBody);

        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.getCustomerId())));
        assertThat(actualUser.getRoles(), is(equalTo(expectedRoles)));
    }

    @Test
    void shouldCreateUserWithRequestedRolesWhenInputContainsNationalIdNumberInstitutionIdAndSetOfRoles()
        throws IOException {

        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = fetchSomeCustomerForThePerson(person);

        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getCristinId());

        var request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);
        var response = sendRequest(request, UserDto.class);

        var actualUser = identityService.listUsers(customer.getId())
                             .stream().collect(SingletonCollector.collect());

        var expectedRoles = createExpectedRoleSet(requestBody);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(actualUser.getInstitution(), is(equalTo(customer.getId())));
        assertThat(actualUser.getRoles(), is(equalTo(expectedRoles)));
    }

    @Test
    void shouldReturnOkWhenTryingToCreateExistingUser() throws IOException {
        var person = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId());

        var request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);
        sendRequest(request, UserDto.class);
        outputStream = new ByteArrayOutputStream();
        request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);
        var response = sendRequest(request, UserDto.class);
        System.out.println(response.getBody());
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldDenyAccessToUsersThatAreDoNotHaveTheRightToCreateUsers() throws IOException {
        var requestBody = new CreateUserRequest(randomPerson(), randomUri(), randomRoles());
        var request = createRequestWithoutAccessRights(requestBody);
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldDenyAccessToUsersThatDoNotHaveTheRightToAddAnyUserAndTheyAreTryingToAddUsersForAnotherInstitution()
        throws IOException {
        var requestBody = new CreateUserRequest(randomPerson(), randomUri(), randomRoles());
        var customer = CustomerDto.builder().withId(requestBody.getCustomerId()).build();
        var request = createRequest(requestBody, customer);

        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldAllowAccessToApplicationAdministrators() throws IOException {
        var person = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId());
        var request = createRequest(requestBody, customer, ADMINISTRATE_APPLICATION);

        var response = sendRequest(request, UserDto.class);

        var actualUser = response.getBodyObject(UserDto.class);
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.getCustomerId())));
    }

    @Test
    void shouldAllowAccessToBackendServices()
        throws IOException {
        var person = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId());
        var request = createBackendRequest(requestBody);

        var response = sendRequest(request, UserDto.class);

        var actualUser = response.getBodyObject(UserDto.class);
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.getCustomerId())));
    }

    @Test
    void shouldNotOverwriteRolesOfExistingUsers() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody = new CreateUserRequest(person, customer.getId(), randomRoles());

        var request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);
        var response = sendRequest(request, UserDto.class);
        final var firstRoles = response.getBodyObject(UserDto.class).getRoles();

        outputStream = new ByteArrayOutputStream();
        //sending a create user request for the same user but with different roles
        requestBody = new CreateUserRequest(person, customer.getId(), randomRoles());
        request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);
        response = sendRequest(request, UserDto.class);
        var allRoles = response.getBodyObject(UserDto.class).getRoles();

        assertThat(allRoles, hasItems(firstRoles.toArray(RoleDto[]::new)));
    }

    @Test
    void shouldReturnConflictErrorWhenPersonHasNoActiveAffiliations() throws IOException {
        var person = scenarios.personWithExactlyOneInactiveEmployment();
        var customer = randomCustomer();
        var requestBody = new CreateUserRequest(person, customer.getId(), randomRoles());
        var request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);

        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));

        var message = response.getBodyObject(Problem.class).getDetail();
        assertThat(message, matchesPattern(".*Person (.*) has no active affiliations with an institution.*"));
    }

    @Test
    void shouldReturnConflictErrorWhenPersonHasActiveAffiliationsButNotWithAnExistingCustomer() throws IOException {
        var person = scenarios.personWithExactlyOneActiveEmploymentInNonCustomer();
        var customer = randomCustomer();
        var requestBody = new CreateUserRequest(person, customer.getId(), randomRoles());
        var request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);

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
        var requestBody = new CreateUserRequest(person, customer.getId(), randomRoles());
        var request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);

        var response = sendRequest(request, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));

        var message = response.getBodyObject(Problem.class).getDetail();
        assertThat(message,
                   matchesPattern(".*Person (.*) is not registered in the Person Registry.*"));
    }

    private CustomerDto randomCustomer() {
        return CustomerDto.builder()
                   .withId(randomUri())
                   .withCristinId(randomUri())
                   .build();
    }

    private Set<RoleDto> createExpectedRoleSet(CreateUserRequest requestBody) {
        return Stream.of(requestBody.getRoles(),
                         Set.of(UserEntriesCreatorForPerson.ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION))
                   .flatMap(Collection::stream)
                   .collect(Collectors.toSet());
    }

    private InputStream createBackendRequest(CreateUserRequest requestBody)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
                   .withScope(RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE)
                   .withBody(requestBody)
                   .build();
    }

    private CreateUserRequest sampleRequestForExistingPersonCustomerAndRoles(String personNin,
                                                                             URI customerId) {
        return new CreateUserRequest(personNin, customerId, randomRoles());
    }

    private CustomerDto fetchSomeCustomerForThePerson(String personNin) {
        var cristinIds = scenarios.getCristinUriForInstitutionAffiliations(personNin, true);

        LOGGER.info("Cristin ids: {}", cristinIds);

        var customer = cristinIds.stream()
                           .map(attempt(institution -> customerService.getCustomerByCristinId(institution)))
                           .map(Try::orElseThrow)
                           .findFirst()
                           .orElseThrow();

        LOGGER.info("Using customer {} with cristin id {}", customer.getId(), customer.getCristinId());
        return customer;
    }

    private String randomPerson() {
        return randomString();
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

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream createRequestWithoutAccessRights(CreateUserRequest requestBody) throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
                   .withCurrentCustomer(customerId)
                   .withBody(requestBody)
                   .build();
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

    private Set<RoleDto> randomRoles() {
        var role = RoleDto.newBuilder().withRoleName(randomString()).build();
        addRoleToIdentityServiceBecauseNonExistingRolesAreIgnored(role);
        return Set.of(role);
    }

    private void addRoleToIdentityServiceBecauseNonExistingRolesAreIgnored(RoleDto role) {
        try {
            identityService.addRole(role);
        } catch (ConflictException | InvalidInputException e) {
            throw new RuntimeException(e);
        }
    }
}
