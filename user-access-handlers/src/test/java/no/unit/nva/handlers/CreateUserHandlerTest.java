package no.unit.nva.handlers;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.ADMINISTRATE_APPLICATION;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_USERS;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.handlers.models.CreateUserRequest;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.userceation.testing.cristin.PeopleAndInstitutions;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfoConstants;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class CreateUserHandlerTest extends HandlerTest {

    private CreateUserHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;
    private LocalCustomerServiceDatabase customerServiceDatabase;
    private LocalIdentityService identityServiceDatabase;
    private IdentityServiceImpl identityService;
    private CustomerService customerService;
    private PeopleAndInstitutions peopleAndInstitutions;

    @BeforeEach
    public void init() {
        setupCustomerService();
        setupIdentityService();
        peopleAndInstitutions = new PeopleAndInstitutions(customerService, identityService);

        var cristinClient = peopleAndInstitutions.createCristinClient();
        var userCreator = new UserEntriesCreatorForPerson(customerService, cristinClient, identityService);
        handler = new CreateUserHandler(userCreator, identityService);
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    public void close() {
        customerServiceDatabase.deleteDatabase();
        identityServiceDatabase.closeDB();
    }

    @Test
    void shouldCreateUserWithRequestedRolesWhenInputContainsNationalIdNumberNvaCustomerIdAndSetOfRoles()
        throws IOException {

        NationalIdentityNumber person = peopleAndInstitutions.getPersonWithSomeActiveAndSomeInactiveAffiliations();
        var someCustomer = fetchSomeCustomerForThePerson(person);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, someCustomer.getId());

        var request = createRequest(requestBody, someCustomer, EDIT_OWN_INSTITUTION_USERS);
        var response = sendRequest(request, UserDto.class);

        var actualUser = identityService.listUsers(requestBody.getCustomerId())
            .stream().collect(SingletonCollector.collect());

        var expectedRoles = createExpectedRoleSet(requestBody);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.getCustomerId())));
        assertThat(actualUser.getRoles(), is(equalTo(expectedRoles)));
    }

    @Test
    void shouldCreateUserWithRequestedRolesWhenInputContainsNationalIdNumberInstitutionIdAndSetOfRoles()
        throws IOException, ApiGatewayException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneActiveAffiliation();
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
        var person = peopleAndInstitutions.getPersonWithSomeActiveAndSomeInactiveAffiliations();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId());

        var request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);
        sendRequest(request, UserDto.class);
        outputStream = new ByteArrayOutputStream();
        request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);
        var response = sendRequest(request, UserDto.class);
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
    void shouldAllowAccessToApplicationAdministrators()
        throws IOException {
        var person = peopleAndInstitutions.getPersonWithSomeActiveAndSomeInactiveAffiliations();
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
        var person = peopleAndInstitutions.getPersonWithSomeActiveAndSomeInactiveAffiliations();
        var customer = fetchSomeCustomerForThePerson(person);
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles(person, customer.getId());
        var request = createBackendRequest(requestBody);
        var response = sendRequest(request, UserDto.class);
        var actualUser = response.getBodyObject(UserDto.class);
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.getCustomerId())));
    }

    @Test
    void shouldNotOverwriteRolesOfExistingUsers() throws ApiGatewayException, IOException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneActiveAffiliation();
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
    void shouldReturnConflictErrorWhenPersonHasNoActiveAffiliations()
        throws ConflictException, NotFoundException, IOException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneInactiveAffiliation();
        var customer = randomCustomer();
        var requestBody = new CreateUserRequest(person, customer.getId(), randomRoles());
        var request = createRequest(requestBody, customer, EDIT_OWN_INSTITUTION_USERS);
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
        var message = response.getBodyObject(Problem.class).getDetail();
        assertThat(message, matchesPattern(".*Person (.*) has no active affiliations with an institution.*"));
    }

    @Test
    void shouldReturnConflictErrorWhenPersonHasActiveAffiliationsButNotWithAnExistingCustomer()
        throws IOException {
        var person = peopleAndInstitutions.getPersonAffiliatedWithNonNvaCustomerInstitution();
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
    void shouldReturnConflictErrorWhenPersonIsNotRegisteredInPersonRegistry()
        throws IOException {
        var person = peopleAndInstitutions.getPersonThatIsNotRegisteredInPersonRegistry();
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

    private CreateUserRequest sampleRequestForExistingPersonCustomerAndRoles(NationalIdentityNumber nin,
                                                                             URI customerId) {
        return new CreateUserRequest(nin, customerId, randomRoles());
    }

    private CustomerDto fetchSomeCustomerForThePerson(NationalIdentityNumber person) {
        return peopleAndInstitutions.getInstitutions(person)
            .stream()
            .map(attempt(institution -> customerService.getCustomerByCristinId(institution)))
            .map(Try::orElseThrow)
            .findFirst()
            .orElseThrow();
    }

    private NationalIdentityNumber randomPerson() {
        return new NationalIdentityNumber(randomString());
    }

    private void setupIdentityService() {
        identityServiceDatabase = new LocalIdentityService();
        identityService = identityServiceDatabase.createDatabaseServiceUsingLocalStorage();
    }

    private void setupCustomerService() {
        customerServiceDatabase = new LocalCustomerServiceDatabase();
        customerServiceDatabase.setupDatabase();
        customerService = new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream createRequestWithoutAccessRights(CreateUserRequest requestBody) throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
            .withCustomerId(customerId)
            .withAccessRights(customerId, randomString())
            .withBody(requestBody)
            .build();
    }

    private InputStream createRequest(CreateUserRequest requestBody,
                                      CustomerDto customer,
                                      AccessRight... accessRights)
        throws JsonProcessingException {
        var accessRightStrings = Arrays.stream(accessRights)
            .map(AccessRight::toString)
            .toArray(String[]::new);
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
            .withCustomerId(customer.getId())
            .withAccessRights(customer.getId(), accessRightStrings)
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
