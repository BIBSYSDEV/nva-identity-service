package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
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
        peopleAndInstitutions = new PeopleAndInstitutions(customerService);
        this.httpClient = AuthorizedBackendClient.prepareWithBearerToken(WiremockHttpClient.create(), randomString());

        seteupPersonAndIsntituttionRegistryClient();
        userCreator = new UserEntriesCreatorForPerson(customerService, cristinClient,
                                                      identityService);
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
    void shouldCreateUserForInstWhenPersonExistsAndInstIsNvaCustomerAndPersonHasSingleActiveAffiliation() {

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
    void shouldNotCreateUserForInstWhenPersonExistsAndInstIsNvaCustomerAndPersonHasSingleInactiveAffiliation() {
        var person = peopleAndInstitutions.getPersonWithExactlyOneInActiveAffiliation();
        var personInfo = userCreator.collectPersonInformation(person);
        var users = userCreator.createUsers(personInfo);
        assertThat(users, is(empty()));
    }

    private void setupCustomerAndIdentityService() {
        customerServiceDatabase = new LocalCustomerServiceDatabase();
        customerServiceDatabase.setupDatabase();
        identityServiceDatabase = new LocalIdentityService();
        identityService = identityServiceDatabase.createDatabaseServiceUsingLocalStorage();
        customerService = new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());
    }

    private void seteupPersonAndIsntituttionRegistryClient() {
        cristinClient = new CristinClient(peopleAndInstitutions.getPersonAndInstitutionRegistryUri(), httpClient);
    }
}