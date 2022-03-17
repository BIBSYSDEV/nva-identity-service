package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.BELONGS_TO;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.FEIDE_ID;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.NIN_FON_NON_FEIDE_USERS;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.ORG_FEIDE_DOMAIN;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.cognito.cristin.person.CristinIdentifier;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDtoWithoutContext;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.CustomerDynamoDBLocal;
import no.unit.nva.database.DatabaseAccessor;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class IdentityServiceEntryUpdateHandlerTest {

    public static final HttpClient HTTP_CLIENT = WiremockHttpClient.create();
    public static final boolean INCLUDE_INACTIVE = true;
    public static final String AT = "@";
    private static final boolean EXCLUDE_INACTIVE = false;
    public static final boolean INCLUDE_ONLY_ACTIVE = false;
    public static final String NOT_EXISTIN_VALUE_IN_LEGACY_ENTRIES = null;
    private static final URI NOT_EXISTIN_URI_IN_LEGACY_ENTRIES = null;
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
    private FakeCognito congitoClient;

    @BeforeEach
    public void init() {
        setUpWiremock();
        this.congitoClient = new FakeCognito();
        this.dataporten = new DataportenMock(httpServer, congitoClient);

        setupCustomerService();
        setupIdentityService();

        registeredPeople = new RegisteredPeopleInstance(httpServer, dataporten, customerService, identityService);
        nvaDataGenerator = new NvaDataGenerator(registeredPeople, customerService);

        var cognitoHost = this.serverUri;
        var cristinHost = this.serverUri;
        handler = new IdentityServiceEntryUpdateHandler(congitoClient,
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
        userAccessDynamoDbLocal.closeDB();
        congitoClient=null;
    }

    @ParameterizedTest(name = "should create user for the person's institution (top org) when person has not logged "
                              + "in before and has one active affiliation")
    @EnumSource(LoginEventType.class)
    void shouldCreateUserForPersonsTopOrganizationWhenPersonHasNotLoggedInBeforeAndHasOneActiveAffiliation(
        LoginEventType loginEventType) {

        var personLoggingIn = registeredPeople.personWithExactlyOneActiveAffiliation();
        var event = randomEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);
        List<UserDto> allUsers = scanAllUsers();
        assertThatUserIsSearchableByCristinCredentials(personLoggingIn, allUsers);
    }

    @ParameterizedTest(name = "should not create user for the person's institution (top org) when person has not "
                              + "logged in before and has only inactive affiliations")
    @EnumSource(value = LoginEventType.class, names = {"NON_FEIDE"})
    void shouldCreateUserForPersonsTopOrganizationWhenPersonHasNotLoggedInBeforeAndHasOnlyInactiveAffiliations(
        LoginEventType loginEventType) {

        var personLoggingIn = registeredPeople.personWithOnlyInactiveAffiliations();
        var event = randomEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);

        var expectedUsers = Collections.<String>emptyList();
        var actualUsers = scanAllUsers();
        assertThat(actualUsers, containsInAnyOrder(expectedUsers.toArray()));
    }

    @ParameterizedTest(name = "should not create user for institutions (top orgs) that the user has active "
                              + "affiliations "
                              + "with when person has not logged int and has active and inactive affiliations")
    @EnumSource(LoginEventType.class)
    void shouldNotCreateUsersForPersonsActiveTopOrgsWhenPersonHasNotLoggedInBeforeAndHasActiveAndInactiveAffiliations(
        LoginEventType loginEventType) {

        var personLoggingIn = registeredPeople.personWithActiveAndInactiveAffiliations();
        var event = randomEvent(personLoggingIn, loginEventType);

        handler.handleRequest(event, context);

        var cristinPersonId = registeredPeople.getCristinPersonId(personLoggingIn);
        var expectedCustomers = registeredPeople.getTopLevelOrgsForPerson(personLoggingIn, EXCLUDE_INACTIVE);
        var expectedUsers = expectedCustomers
            .stream()
            .map(customerCristinId -> fetchUserFromDatabase(cristinPersonId, customerCristinId))
            .toArray(UserDto[]::new);
        var actualUsers = scanAllUsers();

        assertThat(expectedUsers.length, is(equalTo(expectedCustomers.size())));
        assertThat(actualUsers, containsInAnyOrder(expectedUsers));
    }

    @ParameterizedTest(name = " should not alter user entries for institutions (top orgs) that the user has already "
                              + "logged in for both valid and invalid affiliations")
    @EnumSource(LoginEventType.class)
    void shouldMaintainPreexistingUserEntriesForBothValidAndInvalidAffiliations(LoginEventType eventType) {
        var personLoggingIn = registeredPeople.personWithActiveAndInactiveAffiliations();
        var alreadyExistingUsers = createUsersForActiveAndInactiveAffiliations(personLoggingIn);
        handler.handleRequest(randomEvent(personLoggingIn, eventType), context);
        var actualUsers = scanAllUsers();
        assertThat(actualUsers, containsInAnyOrder(alreadyExistingUsers.toArray(UserDto[]::new)));
    }

    @ParameterizedTest(name = "should return access rights as user groups for user concatenated with customer cristin "
                              + "identifier for user's active top orgs")
    @EnumSource(LoginEventType.class)
    void shouldReturnAccessRightsForUserConcatenatedWithCustomerCristinIdentifierForUsersActiveTopOrgs(
        LoginEventType eventType) {
        var personLoggingIn = registeredPeople.personWithActiveAndInactiveAffiliations();
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
        LoginEventType eventType) {
        var personLoggingIn = registeredPeople.personWithActiveAndInactiveAffiliations();
        var preExistingUsers = createUsersForActiveAndInactiveAffiliations(personLoggingIn);
        var expectedUsers = preExistingUsers.stream()
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

    @Test
    void shouldMaintainFeideIdAsUsernameForLegacyEntriesAndUpdateAllExternalIdentifierFields() {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        final String orgFeideDomain = feideDomainOfUsersInstitution(person);
        var personsFeideIdentifier = randomString() + AT + orgFeideDomain;
        var preExistingUser = legacyUserWithFeideIdentifierAsUsername(person, personsFeideIdentifier);

        CognitoUserPoolPreTokenGenerationEvent loginEvent =
            randomEventOfFeideUser(person, personsFeideIdentifier, orgFeideDomain);
        handler.handleRequest(loginEvent, context);

        var updatedUser = identityService.getUser(preExistingUser);

        assertThatNewFieldsAreUpdatedForLegacyUserEntries(person, personsFeideIdentifier, updatedUser);
    }

    @ParameterizedTest(name = "should add customer id as current-customer-id when user logs in and has only one "
                              + "active affiliation")
    @EnumSource(LoginEventType.class)
    void shouldAddCustomerIdAsChosenCustomerIdWhenUserLogsInAndHasOnlyOneActiveAffiliation(
        LoginEventType loginEventType) {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        var expectedCustomerCristinId = registeredPeople.getTopLevelOrgsForPerson(person, INCLUDE_ONLY_ACTIVE)
            .stream().collect(SingletonCollector.collect());

        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var expectedCurrentCustomerId = customerService.getCustomerByCristinId(expectedCustomerCristinId).getId();

        var actualCustomerId = fetchCurrentCustomClaimForCongitoUserUpdate();
        assertThat(actualCustomerId, is(equalTo(expectedCurrentCustomerId.toString())));
    }

    @ParameterizedTest(name = " should add Feide specified customer id as current customer id when user logs in with "
                              + "feide")
    @EnumSource(value = LoginEventType.class, names = {"FEIDE"}, mode = Mode.INCLUDE)
    void shouldAddFeideSpecifiedCustomerIdAsCurrentCustomerIdWhenUserLogsInWithFeide(
        LoginEventType loginEventType) {
        var person = registeredPeople.personWithManyActiveAffiliations();

        var event = randomEvent(person, loginEventType);
        var customersFeideDomain = event.getRequest().getUserAttributes().get(ORG_FEIDE_DOMAIN);
        var expectedCustomerId = fetchCustomerBasedOnFeideDomain(customersFeideDomain);
        handler.handleRequest(event, context);

        var actualCustomerId = fetchCurrentCustomClaimForCongitoUserUpdate();
        assertThat(actualCustomerId, is(equalTo(expectedCustomerId.toString())));
    }

    @ParameterizedTest(name = "should not update customerId when user has many affiliations and logs in with personal"
                              + " number")
    @EnumSource(value = LoginEventType.class, names = {"NON_FEIDE"}, mode = Mode.INCLUDE)
    void shouldNotUpdateCurrentCustomerIdWenUserHasManyAffilationsaAdLogsInWithPersonalNumber(
        LoginEventType loginEventType) {
        var person = registeredPeople.personWithManyActiveAffiliations();

        var event = randomEvent(person, loginEventType);

        handler.handleRequest(event, context);

        var actualCustomerId = fetchCurrentCustomClaimForCongitoUserUpdate();
        assertThat(actualCustomerId, is(nullValue()));
    }

    private URI fetchCustomerBasedOnFeideDomain(String customersFeideDomain) {
        return customerService.getCustomers().stream()
            .filter(customer -> customersFeideDomain.equals(customer.getFeideOrganizationDomain()))
            .map(CustomerDtoWithoutContext::getId)
            .collect(SingletonCollector.collectOrElse(null));
    }

    private String fetchCurrentCustomClaimForCongitoUserUpdate() {
        var request = congitoClient.getUpdateUserRequest();
        return request.userAttributes()
            .stream()
            .filter(a -> a.name().equals(CURRENT_CUSTOMER_CLAIM))
            .map(AttributeType::value)
            .collect(SingletonCollector.collectOrElse(null));
    }

    private void assertThatNewFieldsAreUpdatedForLegacyUserEntries(NationalIdentityNumber person,
                                                                   String personsFeideIdentifier,
                                                                   UserDto updatedUser) {
        assertThat(updatedUser.getCristinId(), is(equalTo(registeredPeople.getCristinPersonId(person))));
        assertThat(updatedUser.getFeideIdentifier(), is(equalTo(personsFeideIdentifier)));
        var expectedTopLevelOrgId = registeredPeople.getTopLevelOrgsForPerson(person, INCLUDE_ONLY_ACTIVE)
            .stream().collect(SingletonCollector.collect());
        assertThat(updatedUser.getInstitutionCristinId(), is(equalTo(expectedTopLevelOrgId)));
    }

    private UserDto legacyUserWithFeideIdentifierAsUsername(NationalIdentityNumber person,
                                                            String personsFeideIdentifier) {
        var preExistingUser = nvaDataGenerator.createUsers(person, INCLUDE_ONLY_ACTIVE)
            .stream().collect(SingletonCollector.collect());
        preExistingUser.setUsername(personsFeideIdentifier);
        preExistingUser.setFeideIdentifier(NOT_EXISTIN_VALUE_IN_LEGACY_ENTRIES);
        preExistingUser.setInstitutionCristinId(NOT_EXISTIN_URI_IN_LEGACY_ENTRIES);
        preExistingUser.setCristinId(NOT_EXISTIN_URI_IN_LEGACY_ENTRIES);
        identityService.addUser(preExistingUser);
        return preExistingUser;
    }

    private String feideDomainOfUsersInstitution(NationalIdentityNumber person) {
        var topLeveOrg = registeredPeople.getTopLevelOrgsForPerson(person, INCLUDE_ONLY_ACTIVE)
            .stream()
            .collect(SingletonCollector.collect());
        return customerService.getCustomerByCristinId(topLeveOrg).getFeideOrganizationDomain();
    }

    private UserDto fetchUserFromDatabase(URI cristinPersonId, URI customerCristinId) {
        return identityService.getUserByCristinIdAndCristinOrgId(cristinPersonId, customerCristinId);
    }


    private void assertThatUserIsSearchableByCristinCredentials(NationalIdentityNumber personLoggingIn,
                                                                List<UserDto> allUsers) {
        assertThatUsersCristinIsPersonsCristinId(personLoggingIn, allUsers);
        assertThatUsersInstitutionCristinIdIsCustomersCristinId(personLoggingIn, allUsers);
    }

    private void assertThatUsersInstitutionCristinIdIsCustomersCristinId(NationalIdentityNumber personLoggingIn,
                                                                         List<UserDto> allUsers) {
        var expectedCustomerCristinIds =
            constructExpectedCustomersFromMockData(registeredPeople, personLoggingIn);
        var actualCustomerCristinIds = allUsers.stream()
            .map(UserDto::getInstitutionCristinId)
            .collect(Collectors.toList());
        assertThat(actualCustomerCristinIds, containsInAnyOrder(expectedCustomerCristinIds.toArray(URI[]::new)));
    }

    private void assertThatUsersCristinIsPersonsCristinId(NationalIdentityNumber personLoggingIn,
                                                          List<UserDto> allUsers) {
        var cristinPersonId = registeredPeople.getCristinPersonId(personLoggingIn);
        for (var user : allUsers) {
            assertThat(user.getCristinId(), is(equalTo(cristinPersonId)));
        }
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

    private List<UserDto> scanAllUsers() {
        ScanDatabaseRequestV2 scanRequest = new ScanDatabaseRequestV2();
        var scanResult = identityService.fetchOnePageOfUsers(scanRequest);

        var allUsers = new ArrayList<>(scanResult.getRetrievedUsers());
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
            .map(customerId -> UriWrapper.fromUri(customerId).getLastPathElement())
            .map(customerIdentifier -> cristinIdentifier.getValue() + BELONGS_TO + customerIdentifier);
    }

    private List<URI> constructExpectedCustomersFromMockData(RegisteredPeopleInstance registeredPeopleInstance,
                                                             NationalIdentityNumber personLoggingIn) {
        return registeredPeopleInstance.getTopLevelOrgsForPerson(personLoggingIn, EXCLUDE_INACTIVE)
            .stream()
            .map(cristinId -> customerService.getCustomerByCristinId(cristinId))
            .map(CustomerDto::getCristinId)
            .collect(Collectors.toList());
    }

    private CognitoUserPoolPreTokenGenerationEvent randomEvent(NationalIdentityNumber nin,
                                                               LoginEventType loginEventType) {
        if (LoginEventType.FEIDE == loginEventType) {
            String feideId = registeredPeople.getFeideIdentifierForPerson();
            String orgFeideDomain = registeredPeople.getSomeFeideOrgIdentifierForPerson(nin);
            return randomEventOfFeideUser(nin, feideId, orgFeideDomain);
        }
        return randomEventOfNonFeideUser(nin);
    }

    private CognitoUserPoolPreTokenGenerationEvent randomEventOfFeideUser(NationalIdentityNumber nin,
                                                                          String feideId,
                                                                          String orgFeideDomain) {

        Map<String, String> userAttributes = Map.of(FEIDE_ID, feideId,
                                                    NIN_FOR_FEIDE_USERS, nin.getNin(),
                                                    ORG_FEIDE_DOMAIN, orgFeideDomain);
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