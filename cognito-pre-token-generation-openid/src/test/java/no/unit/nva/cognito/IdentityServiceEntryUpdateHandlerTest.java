package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.BELONGS_TO;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.FEIDE_ID;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.NIN_FON_NON_FEIDE_USERS;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomVocabularies;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Request;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.CustomerDynamoDBLocal;
import no.unit.nva.database.DatabaseAccessor;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class IdentityServiceEntryUpdateHandlerTest {

    public static final String RANDOM_NIN = randomString();
    public static final HttpClient HTTP_CLIENT = WiremockHttpClient.create();
    private final Context context = new FakeContext();
    private IdentityServiceEntryUpdateHandler handler;

    private WireMockServer httpServer;
    private URI serverUri;
    private String jwtToken;
    private CustomerDynamoDBLocal customerDynamoDbLocal;
    private DynamoDBCustomerService customerService;
    private DatabaseAccessor userAccessDynamoDbLocal;
    private IdentityService identityService;
    private DataportenMock identityProvider;

    @BeforeEach
    public void init() {
        setUpWiremock();
        this.identityProvider = new DataportenMock(httpServer);
        var cognitoHost = this.serverUri;
        var cristinHost = this.serverUri;

        setupCustomerService();
        setupIdentityService();

        handler = new IdentityServiceEntryUpdateHandler(identityProvider.getCognitoClient(),
                                                        HTTP_CLIENT,
                                                        cognitoHost,
                                                        cristinHost,
                                                        customerService,
                                                        identityService);
    }

    @AfterEach
    public void close() {
        httpServer.stop();
        customerDynamoDbLocal.deleteDatabase();
        userAccessDynamoDbLocal.closeDB();
    }

    @ParameterizedTest(name = "should create one user per organization for every organizations the person has an "
                              + "active affiliation with when person has no users")
    @EnumSource(LoginEventType.class)
    void shouldCreateOneUserPerOrganizationForEveryOrganizationThePersonHasAnActiveAffiliationWithWhenPersonHasNoUsers(
        LoginEventType loginEventType) {
        var registeredPeople =
            new RegisteredPeopleInstance(identityProvider,
                                         customerService,
                                         identityService)
                .generateRandomPeople(3);
        for (var person : registeredPeople.getPeople()) {
            personPerformsLogin(loginEventType, registeredPeople, person);
        }

        for (var person : registeredPeople.getPeople()) {
            assertThatOneUserEntryPerActiveAffiliationPerPersonHasBeenGenerated(registeredPeople, person);
        }
    }

    @ParameterizedTest(name = "should not update existing user entries when persons affiliations have not changed "
                              + "since last login ")
    @EnumSource(LoginEventType.class)
    void shouldNotUpdateExistingUserEntriesWhenPersonsAffiliationsHaveNotChangedSinceLastLogin(
        LoginEventType loginEventType) {
        var registeredPeople =
            new RegisteredPeopleInstance(identityProvider, customerService, identityService)
                .generateRandomPeople(10);
        var userEntriesOfPeopleThatHaveLoggedInBefore =
            createUserEntriesForFirstHalfOfPeople(registeredPeople);

        for (var person : registeredPeople.getPeople()) {
            personPerformsLogin(loginEventType, registeredPeople, person);
        }

        for (var user : userEntriesOfPeopleThatHaveLoggedInBefore) {
            var savedUser = identityService.getUser(user);
            assertThat(savedUser, is(equalTo(user)));
        }
    }

    private List<UserDto> createUserEntriesForFirstHalfOfPeople(RegisteredPeopleInstance registeredPeople) {
        var firstHalfOfPeople =
            registeredPeople.getPeople().subList(0, registeredPeople.getPeople().size() / 2);
        return firstHalfOfPeople.stream()
            .map(registeredPeople::createUserEntriesForPerson)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private void assertThatOneUserEntryPerActiveAffiliationPerPersonHasBeenGenerated(
        RegisteredPeopleInstance registeredPeople, NationalIdentityNumber person) {
        var expectedCustomers =
            registeredPeople.fetchNvaCustomersForPersonsActiveAffiliations(person).collect(Collectors.toList());
        var unexpectedCustomers =
            registeredPeople.fetchNvaCustomersForPersonsInactiveAffiliations(person).collect(Collectors.toList());
        var expectedUsers =
            generateUsersBasedOnRegisteredPeopleInstanceData(registeredPeople, person, expectedCustomers);

        var actualUsers = fetchActualUsersFromIdentityServiceDatabase(expectedCustomers);

        assertThat(actualUsers, containsInAnyOrder(expectedUsers));
        assertThatNoUserHasBeenCreatedForInactiveAffiliations(unexpectedCustomers);
    }

    private void personPerformsLogin(LoginEventType loginEventType, RegisteredPeopleInstance registeredPeople,
                                     NationalIdentityNumber person) {
        var event = randomEvent(person, loginEventType);

        handler.handleRequest(event, context);
    }

    private void assertThatNoUserHasBeenCreatedForInactiveAffiliations(List<URI> unexpectedCustomers) {
        for (var customer : unexpectedCustomers) {
            var users = identityService.listUsers(customer);
            assertThat(users, is(empty()));
        }
    }

    private void setupIdentityService() {
        this.userAccessDynamoDbLocal = new DatabaseAccessor() {
        };
        userAccessDynamoDbLocal.initializeTestDatabase();
        this.identityService = new IdentityServiceImpl(userAccessDynamoDbLocal.getDynamoDbClient());
    }

    private String[] generateUsersBasedOnRegisteredPeopleInstanceData(RegisteredPeopleInstance registeredPeople,
                                                                      NationalIdentityNumber person,
                                                                      List<URI> expectedCustomers) {
        return expectedCustomers.stream()
            .map(UriWrapper::new)
            .map(UriWrapper::getFilename)
            .map(customerIdentifier -> registeredPeople.getCristinIdentifier(person).getValue()
                                       + BELONGS_TO
                                       + customerIdentifier)
            .toArray(String[]::new);
    }

    private List<String> fetchActualUsersFromIdentityServiceDatabase(List<URI> expectedCustomers) {
        return expectedCustomers.stream().map(uri -> identityService.listUsers(uri))
            .flatMap(Collection::stream)
            .map(UserDto::getUsername)
            .collect(Collectors.toList());
    }

    private CognitoUserPoolPreTokenGenerationEvent randomEvent(NationalIdentityNumber nin,
                                                               LoginEventType loginEventType) {
        if (LoginEventType.FEIDE == loginEventType) {
            return randomEventOfFeideUser(nin);
        }
        return randomEventOfNonFeideUser(nin);
    }

    private CognitoUserPoolPreTokenGenerationEvent randomEventOfFeideUser(NationalIdentityNumber nin) {

        Map<String, String> userAttributes = Map.of(FEIDE_ID, randomString(), NIN_FOR_FEIDE_USERS, nin.getNin());
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withUserName(randomString())
            .withRequest(Request.builder().withUserAttributes(userAttributes).build())
            .withCallerContext(CallerContext.builder().withClientId(identityProvider.getClientId()).build())
            .build();
    }

    private CognitoUserPoolPreTokenGenerationEvent randomEventOfNonFeideUser(NationalIdentityNumber nin) {
        Map<String, String> userAttributes = Map.of(NIN_FON_NON_FEIDE_USERS, nin.toString());
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withUserName(randomString())
            .withRequest(Request.builder().withUserAttributes(userAttributes).build())
            .withCallerContext(CallerContext.builder().withClientId(identityProvider.getClientId()).build())
            .build();
    }

    private List<CustomerDto> populateCustomerService(RegisteredPeopleInstance registeredPeopleInstance) {
        return registeredPeopleInstance.getCristinOrgUris().stream()
            .map(this::createCustomer)
            .collect(Collectors.toList());
    }

    private void setupCustomerService() {
        this.customerDynamoDbLocal = new CustomerDynamoDBLocal();
        customerDynamoDbLocal.setupDatabase();
        var localCustomerClient = customerDynamoDbLocal.getDynamoClient();
        this.customerService = new DynamoDBCustomerService(localCustomerClient);
    }

    private CustomerDto createCustomer(URI affiliation) {
        var dto = CustomerDto.builder()
            .withCristinId(affiliation.toString())
            .withVocabularies(randomVocabularies())
            .withArchiveName(randomString())
            .withName(randomString())
            .withDisplayName(randomString())
            .build();
        return customerService.createCustomer(dto);
    }

    private void setUpWiremock() {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        serverUri = URI.create(httpServer.baseUrl());
    }

    private String[] extractActualCustomGroups(CognitoUserPoolPreTokenGenerationEvent response) {
        return response.getResponse()
            .getClaimsOverrideDetails()
            .getGroupOverrideDetails()
            .getGroupsToOverride();
    }
}