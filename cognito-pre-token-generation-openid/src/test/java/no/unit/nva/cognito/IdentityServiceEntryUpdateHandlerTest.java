package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.BELONGS_TO;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.FEIDE_ID;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.NIN_FON_NON_FEIDE_USERS;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Every.everyItem;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Request;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.cognito.cristin.person.CristinIdentifier;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.CustomerDynamoDBLocal;
import no.unit.nva.database.DatabaseAccessor;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class IdentityServiceEntryUpdateHandlerTest {

    public static final HttpClient HTTP_CLIENT = WiremockHttpClient.create();

    private static final boolean EXCLUDE_INACTIVE = false;
    public static final boolean INCLUDE_INACTIVE = true;
    public static final String AT = "@";
    private final Context context = new FakeContext();
    private IdentityServiceEntryUpdateHandler handler;

    private WireMockServer httpServer;
    private URI serverUri;
    private CustomerDynamoDBLocal customerDynamoDbLocal;
    private DynamoDBCustomerService customerService;
    private DatabaseAccessor userAccessDynamoDbLocal;
    private IdentityService identityService;
    private DataportenMock dataporten;
    private RegisteredPeopleInstance registeredPeople;
    private NvaDataGenerator nvaDataGenerator;

    @BeforeEach
    public void init() {
        setUpWiremock();
        this.dataporten = new DataportenMock(httpServer);
        var cognitoHost = this.serverUri;
        var cristinHost = this.serverUri;

        setupCustomerService();
        setupIdentityService();

        registeredPeople = new RegisteredPeopleInstance(httpServer, dataporten, customerService,identityService);
        nvaDataGenerator = new NvaDataGenerator(registeredPeople, customerService);

        handler = new IdentityServiceEntryUpdateHandler(dataporten.getCognitoClient(),
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

    @ParameterizedTest(name = "should create user for the person's institution (top org) when person has not logged "
                              + "in before and has one active affiliation")
    @EnumSource(LoginEventType.class)
    void shouldCreateUserForPersonsTopOrganizationWhenPersonHasNotLoggedInBeforeAndHasOneActiveAffiliation(
        LoginEventType loginEventType) {

        var personLoggingIn = registeredPeople.aPersonWithExactlyOneActiveAffiliation();
        var event = randomEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);

        var expectedCustomerIds =
            constructExpectedCustomersFromMockData(registeredPeople, personLoggingIn);
        var expectedUsernames = constructUsernames(registeredPeople, personLoggingIn, expectedCustomerIds);
        var actualUsernames = scanAllUsers().stream().map(UserDto::getUsername).collect(Collectors.toList());
        assertThat(actualUsernames, containsInAnyOrder(expectedUsernames.toArray()));
    }

    @ParameterizedTest(name = "should not create user for the person's institution (top org) when person has not "
                              + "logged in before and has only inactive affiliations")
    @EnumSource(LoginEventType.class)
    void shouldCreateUserForPersonsTopOrganizationWhenPersonHasNotLoggedInBeforeAndHasOnlyInactiveAffiliations(
        LoginEventType loginEventType) {

        var personLoggingIn = registeredPeople.aPersonWithOnlyInactiveAffiliations();
        var event = randomEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);

        var expectedUsers = Collections.<String>emptyList();
        var actualUsers = scanAllUsers();
        assertThat(actualUsers, containsInAnyOrder(expectedUsers.toArray()));
    }

    @ParameterizedTest(name = "should create user for institutions (top orgs) that the user has active affiliations "
                              + "with when person has not logged int and has active and inactive affiliations")
    @EnumSource(LoginEventType.class)
    void shouldCreateUsersForPersonsActiveTopOrgsWhenPersonHasNotLoggedInBeforeAndHasActiveAndInactiveAffiliations(
        LoginEventType loginEventType) {

        var personLoggingIn = registeredPeople.aPersonWithActiveAndInactiveAffiliations();
        var event = randomEvent(personLoggingIn, loginEventType);

        handler.handleRequest(event, context);

        var personsCristinIdentifier = registeredPeople.getCristinPersonIdentifier(personLoggingIn);
        var expectedCustomers = registeredPeople.getTopLevelOrgsForPerson(personLoggingIn, EXCLUDE_INACTIVE);

        var expectedUsers = expectedCustomers
            .stream()
            .map(customerCristinId -> formatUsername(personsCristinIdentifier, customerCristinId))
            .map(username -> identityService.getUser(UserDto.newBuilder().withUsername(username).build()))
            .toArray(UserDto[]::new);
        var actualUsers = scanAllUsers();
        assertThat(actualUsers, containsInAnyOrder(expectedUsers));
    }

    @ParameterizedTest(name = " should not alter user entries for institutions (top orgs) that the user has already "
                              + "logged in for both valid and invalid affiliations")
    @EnumSource(LoginEventType.class)
    void shouldMaintainPreexistingUserEntriesForBothValidAndInvalidAffiliations(LoginEventType eventType) {
        var personLoggingIn = registeredPeople.aPersonWithActiveAndInactiveAffiliations();
        var personCristinId = registeredPeople.getCristinPersonId(personLoggingIn);
        var alreadyExistingUsers = createUsersForActiveAndInactiveAffiliations(personLoggingIn);
        handler.handleRequest(randomEvent(personLoggingIn, eventType), context);

        var actualUsers = scanAllUsers();
        assertThat(actualUsers, containsInAnyOrder(alreadyExistingUsers.toArray(UserDto[]::new)));
    }

    @ParameterizedTest(name = "should return access rights as user groups for user concatenated with customer cristin "
                              + "identifier for user's active top orgs")
    @EnumSource(LoginEventType.class)
    void shouldReturnAccessRightsForUserConcatenatedWithCustomerCristinIdentifierForUsersActiveTopOrgs(
        LoginEventType eventType) throws InterruptedException {
        var personLoggingIn = registeredPeople.aPersonWithActiveAndInactiveAffiliations();
        var usersForActiveAndInactiveAffiliations = createUsersForActiveAndInactiveAffiliations(personLoggingIn);
        var expectedUsers = usersForActiveAndInactiveAffiliations.stream()
            .filter(user -> userHasActiveAffiliationWithCustomer(user, personLoggingIn))
            .collect(Collectors.toSet());

        var expectedAccessRightsWithCristinIdentifiers = expectedUsers.stream()
            .map(this::createAccessRightsCristinIdVersion)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        var response = handler.handleRequest(randomEvent(personLoggingIn, eventType), context);

        var actualAccessRights =
            response.getResponse().getClaimsOverrideDetails().getGroupOverrideDetails().getGroupsToOverride();
        assertThat(expectedAccessRightsWithCristinIdentifiers, everyItem(in(actualAccessRights)));
    }


    @ParameterizedTest(name = "should return access rights as user groups for user concatenated with customer NVA "
                              + "identifier for user's active top orgs")
    @EnumSource(LoginEventType.class)
    void shouldReturnAccessRightsForUserConcatenatedWithCustomerNvaIdentifierForUsersActiveTopOrgs(
        LoginEventType eventType) throws InterruptedException {
        var personLoggingIn = registeredPeople.aPersonWithActiveAndInactiveAffiliations();
        var usersForActiveAndInactiveAffiliations = createUsersForActiveAndInactiveAffiliations(personLoggingIn);
        var expectedUsers = usersForActiveAndInactiveAffiliations.stream()
            .filter(user -> userHasActiveAffiliationWithCustomer(user, personLoggingIn))
            .collect(Collectors.toSet());

        var expectedAccessRightsWithNvaIdentifiers = expectedUsers.stream()
            .map(this::createAccessRightsNvaVersion)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        var response = handler.handleRequest(randomEvent(personLoggingIn, eventType), context);

        var actualAccessRights =
            response.getResponse().getClaimsOverrideDetails().getGroupOverrideDetails().getGroupsToOverride();
        assertThat(expectedAccessRightsWithNvaIdentifiers, everyItem(in(actualAccessRights)));
    }


    private List<UserDto> createUsersForActiveAndInactiveAffiliations(NationalIdentityNumber personLoggingIn) {
        return nvaDataGenerator.createUsers(personLoggingIn, INCLUDE_INACTIVE)
            .stream()
            .map(user -> identityService.addUser(user))
            .map(user -> identityService.getUser(user))
            .collect(Collectors.toList());
    }

    private List<String> createAccessRightsNvaVersion(UserDto user) {
        var customerIdentifier = customerService.getCustomer(user.getInstitution()).getIdentifier().toString();
        return user.getAccessRights().stream()
            .map(accessRight -> accessRight + AT + customerIdentifier)
            .collect(Collectors.toList());
    }


    private List<String> createAccessRightsCristinIdVersion(UserDto user) {
        var customerCristinId = customerService.getCustomer(user.getInstitution()).getCristinId();
        var customerCristinIdentifier = UriWrapper.fromUri(customerCristinId).getLastPathElement();
        return user.getAccessRights().stream()
            .map(accessRight -> accessRight + AT + customerCristinIdentifier)
            .collect(Collectors.toList());
    }

    private boolean userHasActiveAffiliationWithCustomer(UserDto user, NationalIdentityNumber personsNin) {
        var customersForActiveAffiliations = registeredPeople
            .getCustomersWithActiveAffiliations(personsNin)
            .map(CustomerDto::getId).collect(Collectors.toSet());
        return customersForActiveAffiliations.contains(user.getInstitution());
    }

    private String formatUsername(CristinIdentifier personsCristinIdentifier, URI customerCristinId) {
        var customerIdentifier = new UriWrapper(customerCristinId).getLastPathElement();
        return personsCristinIdentifier.getValue() + BELONGS_TO + customerIdentifier;
    }

    private List<String> constructUsernames(RegisteredPeopleInstance registeredPeopleInstance,
                                            NationalIdentityNumber personLoggingIn, List<URI> expectedCustomerIds) {
        return Stream.of(registeredPeopleInstance.getCristinPersonIdentifier(personLoggingIn))
            .flatMap(cristinIdentifier -> createUsersForPersonAndCustomers(expectedCustomerIds, cristinIdentifier))
            .collect(Collectors.toList());
    }

    private List<UserDto> scanAllUsers() {
        ScanDatabaseRequestV2 scanRequest = new ScanDatabaseRequestV2();
        var allUsers = new ArrayList<UserDto>();
        var scanResult = identityService.fetchOnePageOfUsers(scanRequest);

        allUsers.addAll(scanResult.getRetrievedUsers());
        while (scanResult.thereAreMoreEntries()) {
            Map<String, AttributeValue> nextStartMarker = scanResult.getStartMarkerForNextScan();
            scanResult = identityService.fetchOnePageOfUsers(scanRequest.newScanDatabaseRequest(nextStartMarker));
            allUsers.addAll(scanResult.getRetrievedUsers());
        }
        return allUsers;
    }

    private Stream<String> createUsersForPersonAndCustomers(List<URI> expectedCustomers,
                                                            CristinIdentifier cristinIdentifier) {
        return expectedCustomers.stream()
            .map(customerId -> new UriWrapper(customerId).getLastPathElement())
            .map(customerIdentifier -> cristinIdentifier.getValue() + BELONGS_TO + customerIdentifier);
    }

    private List<URI> constructExpectedCustomersFromMockData(RegisteredPeopleInstance registeredPeopleInstance,
                                                             NationalIdentityNumber personLoggingIn) {
        return registeredPeopleInstance.getTopLevelOrgsForPerson(personLoggingIn, EXCLUDE_INACTIVE)
            .stream()
            .map(cristinId -> customerService.getCustomerByCristinId(cristinId.toString()))
            .map(CustomerDto::getCristinId)
            .map(URI::create)
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
            .withCallerContext(CallerContext.builder().withClientId(dataporten.getClientId()).build())
            .build();
    }

    private CognitoUserPoolPreTokenGenerationEvent randomEventOfNonFeideUser(NationalIdentityNumber nin) {
        Map<String, String> userAttributes = Map.of(NIN_FON_NON_FEIDE_USERS, nin.toString());
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withUserName(randomString())
            .withRequest(Request.builder().withUserAttributes(userAttributes).build())
            .withCallerContext(CallerContext.builder().withClientId(dataporten.getClientId()).build())
            .build();
    }

    private void setupIdentityService() {
        this.userAccessDynamoDbLocal = new DatabaseAccessor() {
        };
        userAccessDynamoDbLocal.initializeTestDatabase();
        this.identityService = new IdentityServiceImpl(userAccessDynamoDbLocal.getDynamoDbClient());
    }

    private void setupCustomerService() {
        this.customerDynamoDbLocal = new CustomerDynamoDBLocal();
        customerDynamoDbLocal.setupDatabase();
        var localCustomerClient = customerDynamoDbLocal.getDynamoClient();
        this.customerService = new DynamoDBCustomerService(localCustomerClient);
    }

    private void setUpWiremock() {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        serverUri = URI.create(httpServer.baseUrl());
    }
}