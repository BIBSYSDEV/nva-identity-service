package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_VALUE;
import static no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson.ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.customPersonRegistry;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
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
import java.util.Collections;
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
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
class UserEntriesCreatorForPersonTest {

    private static final String BOT_FILTER_BYPASS_HEADER_NAME = randomString();
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

        var defaultRequestHeaders = new HttpHeaders()
                                        .withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);
        MockPersonRegistry mockPersonRegistry = new MockPersonRegistry(cristinUsername,
                                                                       cristinPassword,
                                                                       wiremockUri,
                                                                       defaultRequestHeaders);

        personRegistry = customPersonRegistry(httpClient,
                                              wiremockUri,
                                              ServiceConstants.API_DOMAIN,
                                              defaultRequestHeaders,
                                              new SecretsReader(secretsManagerClient));
        scenarios = new AuthenticationScenarios(mockPersonRegistry, customerService, identityService);

        userCreator = new UserEntriesCreatorForPerson(identityService);
    }

    @AfterEach
    public void finish() {
        customerServiceDatabase.deleteDatabase();
        identityServiceDatabase.closeDB();
    }

    // @todo: this is now externalized. create tests for UserCreationContext
    //    @Test
    //    void shouldNotCreateUserWhenThePersonDoesNotExistInPersonRegistry() {
    //        var personNin = scenarios.personThatIsNotRegisteredInPersonRegistry();
    //        var customers = Collections.<CustomerDto>emptySet();
    //        var person = personRegistry.fetchPersonByNin(personNin);
    //        var userCreationContext = new UserCreationContext(person, customers);
    //
    //        assertThrows(IllegalStateException.class, () -> userCreator.createUsers(personInformation, customers));
    //    }

    @Test
    @DisplayName("should create User for Institution when the Person exists in the Person-Registry,"
                 + "and they have an Affiliation with the Institution "
                 + "and the Affiliation is active"
                 + "and the the Institution is an NVA Customer"
                 + "and the Person has no other Affiliations active or inactive"
    )
    void shouldCreateUserForInstWhenPersonExistsAndInstIsNvaCustomerAndPersonHasSingleActiveAffiliation() {
        var personNin = scenarios.personWithExactlyOneActiveEmployment().nin();
        var nin = NationalIdentityNumber.fromString(personNin);
        var person = personRegistry.fetchPersonByNin(nin).orElseThrow();
        var customers = fetchCustomersWithActiveAffiliations(person.getAffiliations());
        var userCreationContext = new UserCreationContext(person, customers);
        var users = userCreator.createUsers(userCreationContext);
        assertThat(users.size(), is(equalTo(1)));
        var actualUser = users.get(SINGLE_USER);
        assertThat(users, contains(actualUser));
        assertThat(actualUser.getCristinId(), is(equalTo(person.getId())));
    }

    @Test
    @DisplayName("should not create User for Institution when the Person exists in the Person-Registry,"
                 + "and they have an Affiliation with the Institution "
                 + "and the Affiliation is inactive"
                 + "and the the Institution is an NVA Customer"
                 + "and the Person has no other Affiliations active or inactive"
    )
    void shouldNotCreateUserForInstWhenPersonExistsAndInstIsNvaCustomerAndPersonHasSingleInactiveAffiliation() {
        var personNin = scenarios.personWithExactlyOneInactiveEmployment().nin();
        var nin = NationalIdentityNumber.fromString(personNin);
        var person = personRegistry.fetchPersonByNin(nin).orElseThrow();
        var customers = fetchCustomersWithActiveAffiliations(person.getAffiliations());
        var userCreationContext = new UserCreationContext(person, customers);
        var users = userCreator.createUsers(userCreationContext);
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
        var personNin = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions().nin();
        var nin = NationalIdentityNumber.fromString(personNin);
        var person = personRegistry.fetchPersonByNin(nin).orElseThrow();
        var customers = fetchCustomersWithActiveAffiliations(person.getAffiliations());
        var userCreationContext = new UserCreationContext(person, customers);
        var expectedCustomers = person.getAffiliations()
                                    .stream()
                                    .map(Affiliation::getInstitutionId)
                                    .map(attempt(institution -> customerService.getCustomerByCristinId(institution)))
                                    .map(Try::orElseThrow)
                                    .map(CustomerDto::getId)
                                    .collect(Collectors.toList());

        var users = userCreator.createUsers(userCreationContext);
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
        var personNin = scenarios.personWithExactlyOneActiveEmployment().nin();
        var nin = NationalIdentityNumber.fromString(personNin);
        var person = personRegistry.fetchPersonByNin(nin).orElseThrow();
        var customers = fetchCustomersWithActiveAffiliations(person.getAffiliations());
        var userCreationContext = new UserCreationContext(person, customers);
        var existingUser = scenarios.createUsersForAllActiveAffiliations(personNin, identityService)
                               .stream()
                               .collect(SingletonCollector.collect());
        var actualUser = userCreator.createUsers(userCreationContext)
                             .stream()
                             .collect(SingletonCollector.collect());
        assertThat(actualUser.getUsername(), is(equalTo(existingUser.getUsername())));
    }

    @Test
    void shouldAddFeideIdentifierWhenFeideIdentifierIsAvailable() {
        var personNin = scenarios.personWithExactlyOneActiveEmployment().nin();
        var nin = NationalIdentityNumber.fromString(personNin);
        var person = personRegistry.fetchPersonByNin(nin).orElseThrow();
        var customers = fetchCustomersWithActiveAffiliations(person.getAffiliations());
        var feideIdentifier = randomString();
        var userCreationContext = new UserCreationContext(person, customers, feideIdentifier);

        var actualUser = userCreator.createUsers(userCreationContext)
                             .stream()
                             .collect(SingletonCollector.collect());
        assertThat(actualUser.getFeideIdentifier(), is(equalTo(feideIdentifier)));
    }

    @Test
    void shouldNotCreateUserForInstitutionThatIsNotAnNvaCustomer() {
        var personNin = scenarios.personWithExactlyOneInactiveEmployment().nin();
        var nin = NationalIdentityNumber.fromString(personNin);
        var person = personRegistry.fetchPersonByNin(nin).orElseThrow();
        var customers = fetchCustomersWithActiveAffiliations(person.getAffiliations());
        var userCreationContext = new UserCreationContext(person, customers);

        var actualUsers = userCreator.createUsers(userCreationContext);
        assertThat(actualUsers, is(emptyIterable()));
    }

    @Test
    void createdUserShouldHaveTheCreatorRoleByDefault() {
        var personNin = scenarios.personWithExactlyOneActiveEmployment().nin();
        var nin = NationalIdentityNumber.fromString(personNin);
        var person = personRegistry.fetchPersonByNin(nin).orElseThrow();
        var customers = fetchCustomersWithActiveAffiliations(person.getAffiliations());
        var userCreationContext = new UserCreationContext(person, customers);
        var actualUser = userCreator.createUsers(userCreationContext)
                             .stream()
                             .collect(SingletonCollector.collect());
        var defaultRoles = actualUser.getRoles();
        assertThat(defaultRoles, contains(ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION));
    }

    @Test
    void shouldUpdateLegacyFeideUserWithNecessaryDetailsWhenSuchUserExists() {
        var personNin = scenarios.personWithExactlyOneActiveEmployment().nin();
        var nin = NationalIdentityNumber.fromString(personNin);
        var feideIdentifier = randomString();
        var existingUser = scenarios.createLegacyUsersForAllActiveAffiliations(personNin,
                                                                               feideIdentifier,
                                                                               identityService)
                               .stream()
                               .collect(SingletonCollector.collect());
        var person = personRegistry.fetchPersonByNin(nin).orElseThrow();
        var customers = fetchCustomersWithActiveAffiliations(person.getAffiliations());
        var userCreationContext = new UserCreationContext(person, customers, feideIdentifier);
        var actualUser = userCreator.createUsers(userCreationContext)
                             .stream()
                             .collect(SingletonCollector.collect());
        var expectedUser = constructExpectedUpdatedUserForLegacyFeideUser(personNin,
                                                                          feideIdentifier,
                                                                          existingUser);

        assertThat(actualUser, samePropertyValuesAs(expectedUser));
    }

    @Test
    void shouldCreateUserForSpecificCustomerInstitutionWhenPersonHasActiveAffiliationWithCustomerInstitution() {
        var personNin = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
        var nin = NationalIdentityNumber.fromString(personNin);
        var person = personRegistry.fetchPersonByNin(nin).orElseThrow();
        var customers = fetchCustomersWithActiveAffiliations(person.getAffiliations());

        assertThatWeHaveMoreThanOneCustomer(customers);

        var selectedCustomer = customers.stream().findAny().orElseThrow();
        var userCreationContext = new UserCreationContext(person, Collections.singleton(selectedCustomer));

        var actualUser = userCreator.createUsers(userCreationContext)
                             .stream()
                             .collect(SingletonCollector.collect());
        assertThat(actualUser.getInstitution(), is(equalTo(selectedCustomer.getId())));
    }

    private Set<CustomerDto> fetchCustomersWithActiveAffiliations(List<Affiliation> affiliations) {
        return affiliations.stream()
                   .map(Affiliation::getInstitutionId)
                   .map(attempt(customerService::getCustomerByCristinId))
                   .flatMap(Try::stream)
                   .collect(Collectors.toSet());
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