package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.io.IOException;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.usercreation.cristin.person.PersonAndInstitutionRegistryClient;
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
    private PersonAndInstitutionRegistryClient personAndInstitutionRegistryClient;
    private AuthorizedBackendClient httpClient;
    private IdentityServiceImpl identityService;
    private DynamoDBCustomerService customerService;

    @BeforeEach
    public void init() {
        setupCustomerAndIdentityService();
        peopleAndInstitutions = new PeopleAndInstitutions(customerService);
        this.httpClient = AuthorizedBackendClient.prepareWithBearerToken(WiremockHttpClient.create(), randomString());

        seteupPersonAndIsntituttionRegistryClient();
        userCreator = new UserEntriesCreatorForPerson(customerService, personAndInstitutionRegistryClient,
                                                      identityService);
    }

    @AfterEach
    public void finish() {
        customerServiceDatabase.deleteDatabase();
        identityServiceDatabase.closeDB();
        peopleAndInstitutions.shutdown();
    }

    @Test
    @DisplayName("should create user for institution when the Person  exists in Person registry,"
                 + "and the Institution is an NVA Customer"
                 + "and the Person has an active affiliation with the institution"
                 + "and the Person has no other affiliations active or inactive")
    void shouldCreateUserForInstWhenPersonIsInPersonExistsAndInstIsNvaCustomerAndPersonHasSingleAffiliation()
        throws IOException, InterruptedException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneActiveAffiliation();
        var personInfo = userCreator.collectPersonInformation(person);
        var users = userCreator.createUsers(personInfo);
        assertThat(users.size(), is(equalTo(1)));
        var actualUser = users.get(SINGLE_USER);
        assertThat(actualUser.getCristinId(), is(equalTo(peopleAndInstitutions.getCristinId(person))));
    }

    private void setupCustomerAndIdentityService() {
        customerServiceDatabase = new LocalCustomerServiceDatabase();
        customerServiceDatabase.setupDatabase();
        identityServiceDatabase = new LocalIdentityService();
        identityService = identityServiceDatabase.createDatabaseServiceUsingLocalStorage();
        customerService = new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());
    }

    private void seteupPersonAndIsntituttionRegistryClient() {
        personAndInstitutionRegistryClient =
            new PersonAndInstitutionRegistryClient(peopleAndInstitutions.getPersonAndInstitutionRegistryUri(),
                                                   httpClient);
    }
}