package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson.ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.userceation.testing.cristin.PeopleAndInstitutions;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserEntriesCreatorForPersonTest {

    public static final int SINGLE_USER = 0;
    private PeopleAndInstitutions peopleAndInstitutions;
    private UserEntriesCreatorForPerson userCreator;
    private LocalCustomerServiceDatabase customerServiceDatabase;
    private LocalIdentityService identityServiceDatabase;
    private CristinClient cristinClient;
    private AuthorizedBackendClient httpClient;
    private IdentityServiceImpl identityService;
    private DynamoDBCustomerService customerService;

    @BeforeEach
    public void init() {
        setupCustomerAndIdentityService();
        peopleAndInstitutions = new PeopleAndInstitutions(customerService, identityService);
        this.httpClient = AuthorizedBackendClient.prepareWithBearerToken(WiremockHttpClient.create(), randomString());

        setupPersonAndInstituttionRegistryClient();
        userCreator = new UserEntriesCreatorForPerson(customerService, cristinClient, identityService);
    }

    @AfterEach
    public void finish() {
        customerServiceDatabase.deleteDatabase();
        identityServiceDatabase.closeDB();
        peopleAndInstitutions.shutdown();
    }

    @Test
    @DisplayName("should create User for Institution when the Person exists in the Person-Registry,"
                 + "and they have an Affiliation with the Institution "
                 + "and the Affiliation is active"
                 + "and the the Institution is an NVA Customer"
                 + "and the Person has no other Affiliations active or inactive"
    )
    void shouldCreateUserForInstWhenPersonExistsAndInstIsNvaCustomerAndPersonHasSingleActiveAffiliation()
        throws ApiGatewayException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneActiveAffiliation();
        var personInfo = userCreator.collectPersonInformation(person);
        var users = userCreator.createUsers(personInfo);
        assertThat(users.size(), is(equalTo(1)));
        var actualUser = users.get(SINGLE_USER);
        assertThat(users, contains(actualUser));
        assertThat(actualUser.getCristinId(), is(equalTo(peopleAndInstitutions.getCristinId(person))));
    }

    @Test
    @DisplayName("should not create User for Institution when the Person exists in the Person-Registry,"
                 + "and they have an Affiliation with the Institution "
                 + "and the Affiliation is inactive"
                 + "and the the Institution is an NVA Customer"
                 + "and the Person has no other Affiliations active or inactive"
    )
    void shouldNotCreateUserForInstWhenPersonExistsAndInstIsNvaCustomerAndPersonHasSingleInactiveAffiliation()
        throws ApiGatewayException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneInactiveAffiliation();
        var personInfo = userCreator.collectPersonInformation(person);
        var users = userCreator.createUsers(personInfo);
        assertThat(users, is(empty()));
    }

    @Test
    @DisplayName(" Given that a Person exists in the Person-Registry,"
                 + "And they have some active Affiliations in some Organizations"
                 + "And the  have some inactive Affiliations is some other Organizations"
                 + "And the parent Institutions are all NVA customers"
                 + "Then it should create a User for each Institution where the Person has an active affiliation with"
    )
    void shouldCreateUsersOnlyForActiveAffiliations() {
        var person = peopleAndInstitutions.getPersonWithSomeActiveAndSomeInactiveAffiliations();
        var personInfo = userCreator.collectPersonInformation(person);
        var expectedCustomers =
            peopleAndInstitutions.getParentIntitutionsWithActiveAffiliations(person)
                .stream()
                .map(attempt(institution -> customerService.getCustomerByCristinId(institution)))
                .map(Try::orElseThrow)
                .map(CustomerDto::getId)
                .collect(Collectors.toList());
        var users = userCreator.createUsers(personInfo);
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
    void shouldNotOverwriteUsernameOfExistingUsers() throws ApiGatewayException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneActiveAffiliation();
        var personInfo = userCreator.collectPersonInformation(person);
        var existingUser = peopleAndInstitutions.createNvaUserForPerson(person);
        var actualUser = userCreator.createUsers(personInfo).stream().collect(SingletonCollector.collect());
        assertThat(actualUser.getUsername(), is(equalTo(existingUser.getUsername())));
    }

    @Test
    void shouldAddFeideIdentifierWhenFeideIdentifierIsAvailable() throws ApiGatewayException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneActiveAffiliation();
        var personFeideIdentifier = randomString();
        var personInfo = userCreator.collectPersonInformation(person, personFeideIdentifier, randomString());
        var actualUser = userCreator.createUsers(personInfo).stream().collect(SingletonCollector.collect());
        assertThat(actualUser.getFeideIdentifier(), is(equalTo(personFeideIdentifier)));
    }

    @Test
    void shouldNotCreateUserForInstitutionThatIsNotAnNvaCustomer() {
        var person = peopleAndInstitutions.getPersonAffiliatedWithNonNvaCustomerInstitution();
        var personInfo = userCreator.collectPersonInformation(person);
        var actualUsers = userCreator.createUsers(personInfo);
        assertThat(actualUsers, is(empty()));
    }

    @Test
    void createdUserShouldHaveTheCreatorRoleByDefault() throws ApiGatewayException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneActiveAffiliation();
        var personInfo = userCreator.collectPersonInformation(person);
        var actualUser = userCreator.createUsers(personInfo).stream().collect(SingletonCollector.collect());
        var defaultRoles = actualUser.getRoles();
        assertThat(defaultRoles, contains(ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION));
    }

    @Test
    void shouldUpdateLegacyFeideUserWithNecessaryDetailsWhenSuchUserExists() throws ApiGatewayException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneActiveAffiliation();
        var feideIdentifier = randomString();
        var existingUser = peopleAndInstitutions.createLegacyNvaUserForPerson(person, feideIdentifier);
        var personInfo = userCreator.collectPersonInformation(person, feideIdentifier, randomString());
        var actualUser = userCreator.createUsers(personInfo).stream().collect(SingletonCollector.collect());
        var expectedUser = constructExpectedUpdatedUserForLegacyFeideUser(person, feideIdentifier, existingUser);

        assertThat(actualUser, samePropertyValuesAs(expectedUser));
    }

    @Test
    void shouldCreateUserForSpecificCustomerInstitutionWhenPersonHasActiveAffiliationWithCustomerInstitution() {
        var person = peopleAndInstitutions.getPersonWithSomeActiveAndSomeInactiveAffiliations();
        var personInfo = userCreator.collectPersonInformation(person);
        var allCustomers = peopleAndInstitutions.getInstitutions(person)
            .stream()
            .map(attempt(institution -> customerService.getCustomerByCristinId(institution)))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
        assertThatWeHaveMoreThanOneCustomer(allCustomers);
        var selectedCustomer = allCustomers.stream().findAny().map(CustomerDto::getId).orElseThrow();
        var actualUser = userCreator.createUser(personInfo, selectedCustomer)
            .stream()
            .collect(SingletonCollector.collect());
        assertThat(actualUser.getInstitution(), is(equalTo(selectedCustomer)));
    }

    @Test
    void shouldLogWarningWhenCristinResponseIsNotOk() {
        var logger = LogUtils.getTestingAppenderForRootLogger();
        var person = peopleAndInstitutions.getPersonThatIsNotRegisteredInPersonRegistry();
        userCreator.collectPersonInformation(person);
        assertThat(logger.getMessages(), matchesPattern(".*Connection to Cristin failed for.*"));
    }

    private void assertThatWeHaveMoreThanOneCustomer(List<CustomerDto> allCustomers) {
        assertThat(allCustomers.size(), is(greaterThan(1)));
    }

    private UserDto constructExpectedUpdatedUserForLegacyFeideUser(NationalIdentityNumber person,
                                                                   String feideIdentifier, UserDto existingUser) {
        var expectedInstitutionCristinId =
            peopleAndInstitutions.getInstitutions(person).stream().collect(SingletonCollector.collect());
        var expectedAffiliation =
            peopleAndInstitutions.getAffiliations(person).stream().collect(SingletonCollector.collect());
        return existingUser.copy()
            .withCristinId(peopleAndInstitutions.getCristinId(person))
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

    private void setupPersonAndInstituttionRegistryClient() {
        cristinClient = new CristinClient(peopleAndInstitutions.getPersonAndInstitutionRegistryUri(), httpClient);
    }
}