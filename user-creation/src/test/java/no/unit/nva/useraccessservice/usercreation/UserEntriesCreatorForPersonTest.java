package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import java.io.IOException;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserEntriesCreatorForPersonTest {

    private PeopleAndInstitutions peopleAndInstitutions;
    private UserEntriesCreatorForPerson userCreator;
    private LocalCustomerServiceDatabase customerServiceDatabase;
    private LocalIdentityService identityServiceDatabase;
    private CristinClient cristinClient;
    private AuthorizedBackendClient httpClient;
    private CristinServerMock cristinServerMock;

    @BeforeEach
    public void init() {
        peopleAndInstitutions = new PeopleAndInstitutions();
        customerServiceDatabase = new LocalCustomerServiceDatabase();
        customerServiceDatabase.setupDatabase();
        identityServiceDatabase = new LocalIdentityService();
        this.httpClient = AuthorizedBackendClient.prepareWithBearerToken(randomString());
        var identityService = identityServiceDatabase.createDatabaseServiceUsingLocalStorage();
        var customerService = new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());

        userCreator = new UserEntriesCreatorForPerson(customerService,
                                                      peopleAndInstitutions.getCristinClient(),
                                                      identityService);
    }

    private void setUpCristinClient() {
        cristinServerMock = new CristinServerMock();
        cristinClient = new CristinClient(cristinServerMock.getServerUri(), httpClient);
    }

    @AfterEach
    public void finish() {
        customerServiceDatabase.deleteDatabase();
        identityServiceDatabase.closeDB();
    }

    @Test
    @DisplayName("should create user for institution "
                 + "when the Person  exists in Person registry,"
                 + "and the Institution is an NVA Customer"
                 + "and the Person has an active affiliation with the institution"
                 + "and the Person has no other affiliations active or inactive")
    void shouldCreateUserForInstWhenPersonIsInPersonExistsAndInstIsNvaCustomerAndPersonHasSingleAffiliation()
        throws IOException, InterruptedException {
        var person = peopleAndInstitutions.getPersonWithExactlyOneActiveAffiliation();
        var personInfo = cristinClient.sendRequestToCristin(person.getNin());
        assertThat(personInfo.getNin(),is(equalTo(person.getNin())));
    }
}