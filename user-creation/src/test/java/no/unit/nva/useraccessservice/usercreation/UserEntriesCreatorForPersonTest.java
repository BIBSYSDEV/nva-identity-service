package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson.ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.customPersonRegistry;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.userceation.testing.cristin.AuthenticationScenarios;
import no.unit.nva.useraccessservice.userceation.testing.cristin.MockPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.logutils.LogUtils;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
class UserEntriesCreatorForPersonTest {

    public static final int SINGLE_USER = 0;
    private UserEntriesCreatorForPerson userCreator;
    private LocalCustomerServiceDatabase customerServiceDatabase;
    private LocalIdentityService identityServiceDatabase;
    private IdentityService identityService;
    private DynamoDBCustomerService customerService;
    private AuthenticationScenarios scenarios;
    private PersonRegistry personRegistry;
    private final FakeSecretsManagerClient secretsManagerClient = new FakeSecretsManagerClient();

    @BeforeEach
    public void init(WireMockRuntimeInfo wireMockRuntimeInfo) throws InvalidInputException, ConflictException {
        setupCustomerAndIdentityService();
        var httpClient = WiremockHttpClient.create();
        var cristinUsername = randomString();
        var cristinPassword = randomString();
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_USERNAME_SECRET_KEY, cristinUsername);
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_PASSWORD_SECRET_KEY, cristinPassword);

        var wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
        MockPersonRegistry mockPersonRegistry = new MockPersonRegistry(cristinUsername, cristinPassword, wiremockUri);

        personRegistry = customPersonRegistry(httpClient, wiremockUri, ServiceConstants.API_DOMAIN,
                                              new SecretsReader(secretsManagerClient));
        scenarios = new AuthenticationScenarios(mockPersonRegistry, customerService, identityService);

        userCreator = new UserEntriesCreatorForPerson(identityService);
    }

    @AfterEach
    public void finish() {
        customerServiceDatabase.deleteDatabase();
        identityServiceDatabase.closeDB();
    }

    private Set<CustomerDto> fetchCustomersWithActiveAffiliations(List<PersonAffiliation> personAffiliations) {
        return personAffiliations.stream()
                   .map(PersonAffiliation::getInstitutionCristinId)
                   .map(attempt(customerService::getCustomerByCristinId))
                   .flatMap(Try::stream)
                   .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("should create User for Institution when the Person exists in the Person-Registry,"
                 + "and they have an Affiliation with the Institution "
                 + "and the Affiliation is active"
                 + "and the the Institution is an NVA Customer"
                 + "and the Person has no other Affiliations active or inactive"
    )
    void shouldCreateUserForInstWhenPersonExistsAndInstIsNvaCustomerAndPersonHasSingleActiveAffiliation() {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var personInformation = new PersonInformationImpl(personRegistry, person);
        var customers = fetchCustomersWithActiveAffiliations(personInformation.getPersonAffiliations());
        var users = userCreator.createUsers(personInformation, customers);
        assertThat(users.size(), is(equalTo(1)));
        var actualUser = users.get(SINGLE_USER);
        assertThat(users, contains(actualUser));
        assertThat(actualUser.getCristinId(), is(equalTo(scenarios.getCristinIdForPerson(person))));
    }

    @Test
    @DisplayName("should not create User for Institution when the Person exists in the Person-Registry,"
                 + "and they have an Affiliation with the Institution "
                 + "and the Affiliation is inactive"
                 + "and the the Institution is an NVA Customer"
                 + "and the Person has no other Affiliations active or inactive"
    )
    void shouldNotCreateUserForInstWhenPersonExistsAndInstIsNvaCustomerAndPersonHasSingleInactiveAffiliation() {
        var person = scenarios.personWithExactlyOneInactiveEmployment();
        var personInformation = new PersonInformationImpl(personRegistry, person);
        var customers = fetchCustomersWithActiveAffiliations(personInformation.getPersonAffiliations());
        var users = userCreator.createUsers(personInformation, customers);
        assertThat(users, is(emptyIterable()));
    }

    @Test
    @DisplayName(" Given that a Person exists in the Person-Registry,"
                 + "And they have some active Affiliations in some Organizations"
                 + "And the  have some inactive Affiliations is some other Organizations"
                 + "And the parent Institutions are all NVA customers"
                 + "Then it should create a User for each Institution where the Person has an active affiliation with"
    )
    void shouldCreateUsersOnlyForActiveAffiliations() {
        var person = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();
        var personInformation = new PersonInformationImpl(personRegistry, person);
        var customers = fetchCustomersWithActiveAffiliations(personInformation.getPersonAffiliations());
        var expectedCustomers =
            scenarios.getCristinUriForInstitutionAffiliations(person, true)
                .stream()
                .map(attempt(institution -> customerService.getCustomerByCristinId(institution)))
                .map(Try::orElseThrow)
                .map(CustomerDto::getId)
                .collect(Collectors.toList());
        var users = userCreator.createUsers(personInformation, customers);
        var actualCustomers = users.stream().map(UserDto::getInstitution).collect(Collectors.toList());
        assertThat(expectedCustomers, containsInAnyOrder(actualCustomers.toArray(URI[]::new)));
    }

    @Test
    @DisplayName(" Given that a Person exists in the Person-Registry,"
                 + "And they have an active affiliation with an Organization"
                 + "And the Organization's parent Institution is an NVA customer"
                 + "And there is already a User for the Person for the Institution"
                 + "Then the username of the existing User is maintained by all means"
    )
    void shouldNotOverwriteUsernameOfExistingUsers() {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var personInformation = new PersonInformationImpl(personRegistry, person);
        var customers = fetchCustomersWithActiveAffiliations(personInformation.getPersonAffiliations());
        var existingUser = scenarios.createUsersForAllActiveAffiliations(person, identityService)
                               .stream()
                               .collect(SingletonCollector.collect());
        var actualUser = userCreator.createUsers(personInformation, customers)
                             .stream()
                             .collect(SingletonCollector.collect());
        assertThat(actualUser.getUsername(), is(equalTo(existingUser.getUsername())));
    }

    @Test
    void shouldAddFeideIdentifierWhenFeideIdentifierIsAvailable() {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var personFeideIdentifier = randomString();
        var personInformation = new PersonInformationImpl(personRegistry, person, personFeideIdentifier,
                                                          randomString());
        var customers = fetchCustomersWithActiveAffiliations(personInformation.getPersonAffiliations());
        var actualUser = userCreator.createUsers(personInformation, customers)
                             .stream()
                             .collect(SingletonCollector.collect());
        assertThat(actualUser.getFeideIdentifier(), is(equalTo(personFeideIdentifier)));
    }

    @Test
    void shouldNotCreateUserForInstitutionThatIsNotAnNvaCustomer() {
        var person = scenarios.personWithExactlyOneInactiveEmployment();
        var personInformation = new PersonInformationImpl(personRegistry, person);
        var customers = fetchCustomersWithActiveAffiliations(personInformation.getPersonAffiliations());
        var actualUsers = userCreator.createUsers(personInformation, customers);
        assertThat(actualUsers, is(emptyIterable()));
    }

    @Test
    void createdUserShouldHaveTheCreatorRoleByDefault() {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var personInformation = new PersonInformationImpl(personRegistry, person);
        var customers = fetchCustomersWithActiveAffiliations(personInformation.getPersonAffiliations());
        var actualUser = userCreator.createUsers(personInformation, customers)
                             .stream()
                             .collect(SingletonCollector.collect());
        var defaultRoles = actualUser.getRoles();
        assertThat(defaultRoles, contains(ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION));
    }

    @Test
    void shouldUpdateLegacyFeideUserWithNecessaryDetailsWhenSuchUserExists() {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var feideIdentifier = randomString();
        var existingUser = scenarios.createLegacyUsersForAllActiveAffiliations(person,
                                                                         feideIdentifier,
                                                                         identityService)
                               .stream()
                               .collect(SingletonCollector.collect());
        var personInformation = new PersonInformationImpl(personRegistry, person);
        var customers = fetchCustomersWithActiveAffiliations(personInformation.getPersonAffiliations());
        var actualUser = userCreator.createUsers(personInformation, customers)
                             .stream()
                             .collect(SingletonCollector.collect());
        var expectedUser = constructExpectedUpdatedUserForLegacyFeideUser(person, feideIdentifier, existingUser);

        assertThat(actualUser, samePropertyValuesAs(expectedUser));
    }

    @Test
    void shouldCreateUserForSpecificCustomerInstitutionWhenPersonHasActiveAffiliationWithCustomerInstitution() {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions();
        var personInformation = new PersonInformationImpl(personRegistry, person);
        var customers = fetchCustomersWithActiveAffiliations(personInformation.getPersonAffiliations());
        assertThatWeHaveMoreThanOneCustomer(customers);
        var selectedCustomer = customers.stream().findAny().map(CustomerDto::getId).orElseThrow();
        var actualUser = userCreator.createUser(personInformation, customers, selectedCustomer)
            .stream()
            .collect(SingletonCollector.collect());
        assertThat(actualUser.getInstitution(), is(equalTo(selectedCustomer)));
    }

    @Test
    void shouldLogWarningWhenCristinResponseIsNotOk() {
        var logger = LogUtils.getTestingAppenderForRootLogger();
        var person = scenarios.failingPersonRegistryRequestBadGateway();
        try {
            new PersonInformationImpl(personRegistry, person);
        } catch (RuntimeException e) {
            // NO-OP
        }
        assertThat(logger.getMessages(), matchesPattern(".*failed with response code 502.*"));
    }

    private void assertThatWeHaveMoreThanOneCustomer(Set<CustomerDto> allCustomers) {
        assertThat(allCustomers, iterableWithSize(greaterThan(1)));
    }

    private UserDto constructExpectedUpdatedUserForLegacyFeideUser(String person,
                                                                   String feideIdentifier,
                                                                   UserDto existingUser) {

        var expectedInstitutionCristinId =
            scenarios.getCristinUriForInstitutionAffiliations(person, true)
                .stream()
                .collect(SingletonCollector.collect());
        var expectedAffiliation = scenarios.getCristinUriForUnitAffiliations(person)
                                      .stream()
                                      .collect(SingletonCollector.collect());
        return existingUser.copy()
            .withCristinId(scenarios.getCristinIdForPerson(person))
            .withInstitutionCristinId(expectedInstitutionCristinId)
            .withFeideIdentifier(feideIdentifier)
            .withAffiliation(expectedAffiliation)
            .build();
    }

    private void setupCustomerAndIdentityService() {
        customerServiceDatabase = new LocalCustomerServiceDatabase();
        customerServiceDatabase.setupDatabase();
        identityServiceDatabase = new LocalIdentityService();
        identityService = identityServiceDatabase.createDatabaseServiceUsingLocalStorage();
        customerService = new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());
    }
}