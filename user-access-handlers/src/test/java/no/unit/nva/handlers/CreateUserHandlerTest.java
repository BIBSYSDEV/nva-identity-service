package no.unit.nva.handlers;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.ADMINISTRATE_APPLICATION;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_USERS;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
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
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles();

        var request = createRequest(requestBody, EDIT_OWN_INSTITUTION_USERS);
        var response = sendRequest(request, UserDto.class);

        var actualUser = identityService.listUsers(requestBody.getCustomerId())
            .stream().collect(SingletonCollector.collect());

        var expectedRoles = createExpectedRoleSet(requestBody);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.getCustomerId())));
        assertThat(actualUser.getRoles(), is(equalTo(expectedRoles)));
    }

    @Test
    void shouldReturnOkWhenTryingToCreateExistingUser() throws IOException {
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles();

        var request = createRequest(requestBody, EDIT_OWN_INSTITUTION_USERS);
        sendRequest(request, UserDto.class);
        outputStream = new ByteArrayOutputStream();
        request = createRequest(requestBody, EDIT_OWN_INSTITUTION_USERS);
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
        var request = createRequest(requestBody);
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldAllowAccessToApplicationAdministrators()
        throws IOException {
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles();
        var request = createRequest(requestBody, ADMINISTRATE_APPLICATION);
        var response = sendRequest(request, UserDto.class);
        var actualUser = response.getBodyObject(UserDto.class);
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.getCustomerId())));
    }

    @Test
    void shouldAllowAccessToBackendServices()
        throws IOException {
        var requestBody = sampleRequestForExistingPersonCustomerAndRoles();
        var request = createBackendRequest(requestBody);
        var response = sendRequest(request, UserDto.class);
        var actualUser = response.getBodyObject(UserDto.class);
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.getCustomerId())));
    }

    @Test
    void shouldNotOverwriteRolesOfExistingUsers() throws NotFoundException, IOException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneActiveAffiliation();
        var customerId = extractCustomerIdOfPersonAffiliation(person);
        var requestBody = new CreateUserRequest(person, customerId, randomRoles());

        var request = createRequest(requestBody, EDIT_OWN_INSTITUTION_USERS);
        var response = sendRequest(request, UserDto.class);
        var firstRoles = response.getBodyObject(UserDto.class).getRoles();

        outputStream = new ByteArrayOutputStream();
        //sending a create user request for the same user but with different roles
        requestBody = new CreateUserRequest(person, customerId, randomRoles());
        request = createRequest(requestBody, EDIT_OWN_INSTITUTION_USERS);
        response = sendRequest(request, UserDto.class);
        var allRoles = response.getBodyObject(UserDto.class).getRoles();

        assertThat(allRoles, hasItems(firstRoles.toArray(RoleDto[]::new)));
    }

    private Set<RoleDto> createExpectedRoleSet(CreateUserRequest requestBody) {
        return Stream.of(requestBody.getRoles(),
                         Set.of(UserEntriesCreatorForPerson.ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private URI extractCustomerIdOfPersonAffiliation(NationalIdentityNumber person) {
        return peopleAndInstitutions.getInstitutions(person).stream()
            .map(attempt(uri -> customerService.getCustomerByCristinId(uri)))
            .map(Try::orElseThrow)
            .map(CustomerDto::getId)
            .collect(SingletonCollector.collect());
    }

    private InputStream createBackendRequest(CreateUserRequest requestBody)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
            .withScope(RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE)
            .withBody(requestBody)
            .build();
    }

    private CreateUserRequest sampleRequestForExistingPersonCustomerAndRoles() {
        var person = peopleAndInstitutions.getPersonWithSomeActiveAndSomeInactiveAffiliations();
        var someCustomer = fetchSomeCustomerForThePerson(person);
        return new CreateUserRequest(person, someCustomer, randomRoles());
    }

    private URI fetchSomeCustomerForThePerson(NationalIdentityNumber person) {
        return peopleAndInstitutions.getInstitutions(person)
            .stream()
            .map(attempt(institution -> customerService.getCustomerByCristinId(institution)))
            .map(Try::orElseThrow)
            .map(CustomerDto::getId)
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

    private InputStream createRequest(CreateUserRequest requestBody, AccessRight... accessRights)
        throws JsonProcessingException {
        var accessRightStrings = Arrays.stream(accessRights)
            .map(AccessRight::toString)
            .toArray(String[]::new);
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
            .withCustomerId(requestBody.getCustomerId())
            .withAccessRights(requestBody.getCustomerId(), accessRightStrings)
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
