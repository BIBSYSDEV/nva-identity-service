package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.unit.nva.auth.AuthorizedBackendClient.prepareWithBearerToken;
import static no.unit.nva.cognito.AuthenticationInformation.COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.AT;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ROLES_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.FEIDE_ID;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.NIN_FON_NON_FEIDE_USERS;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.ORG_FEIDE_DOMAIN;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Request;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.FakeCognito;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class UserSelectionUponLoginHandlerTest {

    public static final HttpClient HTTP_CLIENT = WiremockHttpClient.create();
    public static final boolean INCLUDE_INACTIVE = true;
    public static final boolean ONLY_ACTIVE = false;
    public static final String NOT_EXISTING_VALUE_IN_LEGACY_ENTRIES = null;
    public static final RoleDto ROLE_FOR_USERS_WITH_ACTIVE_AFFILIATION = RoleDto.newBuilder()
        .withRoleName("Creator")
        .build();
    private static final boolean ACTIVE = true;
    private static final URI NOT_EXISTING_URI_IN_LEGACY_ENTRIES = null;
    private final Context context = new FakeContext();
    private UserSelectionUponLoginHandler handler;

    private WireMockServer httpServer;
    private URI serverUri;
    private LocalCustomerServiceDatabase customerDynamoDbLocal;
    private DynamoDBCustomerService customerService;
    private LocalIdentityService userAccessDynamoDbLocal;
    private IdentityService identityService;
    private NvaAuthServerMock authServerMock;
    private RegisteredPeopleInstance registeredPeople;
    private NvaDataGenerator nvaDataGenerator;
    private FakeCognito congitoClient;

    @BeforeEach
    public void init() {
        setUpWiremock();
        this.congitoClient = new FakeCognito(randomString());
        this.authServerMock = new NvaAuthServerMock(httpServer, congitoClient);

        setupCustomerService();
        setupIdentityService();

        var authorizedBackendClient =
            prepareWithBearerToken(HTTP_CLIENT, "Bearer " + authServerMock.getJwtToken());

        registeredPeople = new RegisteredPeopleInstance(httpServer, authServerMock, customerService, identityService);
        nvaDataGenerator = new NvaDataGenerator(registeredPeople, customerService);

        var cristinHost = this.serverUri;
        var cristinClient = new CristinClient(cristinHost, authorizedBackendClient);
        handler = new UserSelectionUponLoginHandler(congitoClient,
                                                    cristinClient,
                                                    customerService,
                                                    identityService);
    }

    @AfterEach
    public void close() {
        httpServer.stop();
        customerDynamoDbLocal.deleteDatabase();
        userAccessDynamoDbLocal.closeDB();
        userAccessDynamoDbLocal.closeDB();
        congitoClient = null;
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

    @ParameterizedTest(name = "should not create user for institutions (top orgs) that the user has inactive "
                              + "affiliations with "
                              + "when person has not logged int and has active and inactive affiliations")
    @EnumSource(LoginEventType.class)
    void shouldNotCreateUsersForPersonsTopOrgsWhenPersonHasNotLoggedInBeforeAndHasActiveAndInactiveAffiliations(
        LoginEventType loginEventType) {

        var personLoggingIn = registeredPeople.personWithActiveAndInactiveAffiliations();
        var event = randomEvent(personLoggingIn, loginEventType);

        handler.handleRequest(event, context);

        var cristinPersonId = registeredPeople.getCristinPersonId(personLoggingIn);
        var expectedCustomers = registeredPeople.getTopLevelOrgsForPerson(personLoggingIn, ONLY_ACTIVE);
        var expectedUsers = expectedCustomers
            .stream()
            .map(customerCristinId -> fetchUserFromDatabase(cristinPersonId, customerCristinId))
            .toArray(UserDto[]::new);
        var actualUsers = scanAllUsers();

        assertThat(expectedUsers.length, is(equalTo(expectedCustomers.size())));
        assertThat(actualUsers, containsInAnyOrder(expectedUsers));
    }

    @ParameterizedTest(name = " should not alter user names for institutions (top orgs) that the user has "
                              + "already logged in for both valid and invalid affiliations")
    @EnumSource(LoginEventType.class)
    void shouldMaintainPreexistingUserEntriesForBothValidAndInvalidAffiliations(LoginEventType eventType) {
        var personLoggingIn = registeredPeople.personWithActiveAndInactiveAffiliations();
        var alreadyExistingUsers = createUsersForAffiliations(personLoggingIn, INCLUDE_INACTIVE);
        var expectedUsernames =
            alreadyExistingUsers.stream().map(UserDto::getUsername).collect(Collectors.toList());
        handler.handleRequest(randomEvent(personLoggingIn, eventType), context);
        var actualUsernames =
            scanAllUsers().stream().map(UserDto::getUsername).collect(Collectors.toList());
        assertThat(actualUsernames, containsInAnyOrder(expectedUsernames.toArray(String[]::new)));
    }

    @ParameterizedTest(name = "should return access rights as user groups for user concatenated with customer NVA "
                              + "identifier for user's active top orgs")
    @EnumSource(LoginEventType.class)
    void shouldReturnAccessRightsForUserConcatenatedWithCustomerNvaIdentifierForUsersActiveTopOrgs(
        LoginEventType eventType) {
        var personLoggingIn = registeredPeople.personWithActiveAndInactiveAffiliations();
        var preExistingUsers = createUsersForAffiliations(personLoggingIn, INCLUDE_INACTIVE);
        var expectedUsers = preExistingUsers.stream()
            .filter(user -> userHasActiveAffiliationWithCustomer(user, personLoggingIn))
            .collect(Collectors.toSet());

        var expectedAccessRightsWithNvaIdentifiers = expectedUsers.stream()
            .map(this::createAccessRightsNvaVersion)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        var response = handler.handleRequest(randomEvent(personLoggingIn, eventType), context);
        var actualAccessRights = extractAccessRights(response);
        assertThat(expectedAccessRightsWithNvaIdentifiers, everyItem(in(actualAccessRights)));
    }

    @Test
    void shouldMaintainFeideIdAsUsernameForLegacyEntriesAndUpdateAllExternalIdentifierFields()
        throws NotFoundException {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        final String orgFeideDomain = feideDomainOfUsersInstitution(person);
        var personsFeideIdentifier = randomString() + AT + orgFeideDomain;
        var preExistingUser = legacyUserWithFeideIdentifierAsUsername(person, personsFeideIdentifier);

        var loginEvent =
            randomEventOfFeideUser(person, personsFeideIdentifier, orgFeideDomain);
        handler.handleRequest(loginEvent, context);

        var updatedUser = identityService.getUser(preExistingUser);

        assertThatNewFieldsAreUpdatedForLegacyUserEntries(person, personsFeideIdentifier, updatedUser);
    }

    @ParameterizedTest(name = "should add customer id as current-customer-id when user logs in and has only one "
                              + "active affiliation")
    @EnumSource(LoginEventType.class)
    void shouldAddCustomerIdAsChosenCustomerIdWhenUserLogsInAndHasOnlyOneActiveAffiliation(
        LoginEventType loginEventType) throws NotFoundException {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        var expectedCustomerCristinId = registeredPeople.getTopLevelOrgsForPerson(person, ONLY_ACTIVE)
            .stream().collect(SingletonCollector.collect());

        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var expectedCurrentCustomerId = customerService.getCustomerByCristinId(expectedCustomerCristinId).getId();

        var actualCustomerId = fetchCurrentCustomClaimForCongitoUserUpdate();
        assertThat(actualCustomerId, is(equalTo(expectedCurrentCustomerId.toString())));
    }

    @ParameterizedTest(name = "should add Feide specified customer id as current customer id when user logs in with "
                              + "feide")
    @EnumSource(value = LoginEventType.class, names = {"FEIDE"}, mode = Mode.INCLUDE)
    void shouldAddFeideSpecifiedCustomerIdAsCurrentCustomerIdWhenUserLogsInWithFeide(
        LoginEventType loginEventType) {
        var person = registeredPeople.personWithManyActiveAffiliations();
        var event = randomEvent(person, loginEventType);
        var customersFeideDomain = event.getRequest().getUserAttributes().get(ORG_FEIDE_DOMAIN);
        var expectedCustomerId = fetchCustomerBasedOnFeideDomain(customersFeideDomain).getId();
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

    @ParameterizedTest(name = "should not assign access rights for active affiliation when customer is not "
                              + "a registered customer in NVA")
    @EnumSource(LoginEventType.class)
    void shouldNotAssignAccessRightsForActiveAffiliationsWhenTopLevelOrgIsNotARegisteredCustomerInNva(
        LoginEventType loginEventType) {
        var person = registeredPeople.personWithActiveAffiliationThatIsNotCustomer();
        var event = randomEvent(person, loginEventType);
        var response = handler.handleRequest(event, context);
        var allUsers = scanAllUsers();
        assertThat(allUsers, is(empty()));
        var accessRights = extractAccessRights(response);
        assertThat(accessRights, is(empty()));
    }

    @ParameterizedTest(name = "should fail when selected user has wrong customer id")
    @EnumSource(LoginEventType.class)
    void shouldFailWhenSelectedUserHasWrongCustomerId(LoginEventType loginEventType) throws NotFoundException {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        var existingUsers = createUsersForAffiliations(person, ONLY_ACTIVE);
        var userWithInvalidCustomerId = existingUsers.get(0);
        var invalidCustomerUri = randomUri();
        userWithInvalidCustomerId.setInstitution(invalidCustomerUri);
        identityService.updateUser(userWithInvalidCustomerId);
        var event = randomEvent(person, loginEventType);
        var exception =
            assertThrows(IllegalStateException.class, () -> handler.handleRequest(event, context));
        assertThat(exception.getMessage(), containsString(COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR));
    }

    @ParameterizedTest(name = "should store all allowed customer IDs in the cognito user attributes")
    @EnumSource(LoginEventType.class)
    void shouldIncludeAllAllowedCustomerIdsInTheCognitoUserCustomer(LoginEventType loginEventType) {
        var person = registeredPeople.personWithActiveAndInactiveAffiliations();
        var expectedCustomerIds = registeredPeople.getTopLevelOrgsForPerson(person, ONLY_ACTIVE)
            .stream()
            .map(attempt(cristinId -> customerService.getCustomerByCristinId(cristinId)))
            .map(Try::orElseThrow)
            .map(CustomerDto::getId)
            .collect(Collectors.toList());
        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualAllowedCustomers = congitoClient.getAdminUpdateUserRequest().userAttributes().stream()
            .filter(a -> a.name().equals(ALLOWED_CUSTOMER_CLAIM))
            .map(AttributeType::value)
            .collect(SingletonCollector.collect());
        for (var expectedCustomerId : expectedCustomerIds) {
            assertThat(actualAllowedCustomers, containsString(expectedCustomerId.toString()));
        }
    }

    @ParameterizedTest(name = "should store all user's roles for each active top level affiliation in cognito user "
                              + "attributes")
    @EnumSource(LoginEventType.class)
    void shouldStoreAllUserRolesForEachActiveTopLevelAffiliationInCognitoUserAttributes(LoginEventType loginEventType) {
        var person = registeredPeople.personWithActiveAndInactiveAffiliations();
        var usersForPerson = createUsersForAffiliations(person, ONLY_ACTIVE);
        var expectedRoleStrings = usersForPerson.stream()
            .map(this::createRoleStrings)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualRoles = congitoClient.getAdminUpdateUserRequest().userAttributes().stream()
            .filter(a -> a.name().equals(ROLES_CLAIM))
            .map(AttributeType::value)
            .collect(SingletonCollector.collect());
        for (var expectedCustomerId : expectedRoleStrings) {
            assertThat(actualRoles, containsString(expectedCustomerId));
        }
    }

    @ParameterizedTest(name = "should store user's cristin Id in cognito user attributes")
    @EnumSource(LoginEventType.class)
    void shouldStorePersonsCristinIdInCognitoUserAttributes(LoginEventType loginEventType) {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        var user = createUsersForAffiliations(person, ONLY_ACTIVE)
            .stream()
            .collect(SingletonCollector.collect());

        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualCristinPersonId = congitoClient.getAdminUpdateUserRequest().userAttributes().stream()
            .filter(a -> a.name().equals(PERSON_CRISTIN_ID_CLAIM))
            .map(AttributeType::value)
            .collect(SingletonCollector.collect());

        assertThat(URI.create(actualCristinPersonId), is(equalTo(user.getCristinId())));
    }

    @ParameterizedTest(name = "should store user's top-level-org affiliation in cognito user attributes when "
                              + "user has only one active affiliation")
    @EnumSource(LoginEventType.class)
    void shouldStoreUsersTopLevelAffiliationWhenUserHasOnlyOneActiveAffiliation(LoginEventType loginEventType) {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        var topLevelAffiliation = registeredPeople.getTopLevelAffiliationsForUser(person, ACTIVE)
            .stream()
            .collect(SingletonCollector.collect());

        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualTopOrgCristinId = getUpdatedClaimFromCognito(TOP_ORG_CRISTIN_ID);

        assertThat(URI.create(actualTopOrgCristinId), is(equalTo(topLevelAffiliation)));
    }

    @ParameterizedTest(name = "should store user's top-level-org affiliation in cognito user attributes when user has "
                              + "many active affiliations but logged in with Feide")
    @EnumSource(value = LoginEventType.class, names = {"FEIDE"}, mode = Mode.INCLUDE)
    void shouldStoreUsersTopLevelAffiliationWhenUserHasManyActiveAffiliationsAndLoggedInWithFeide(
        LoginEventType loginEventType) {
        var person = registeredPeople.personWithManyActiveAffiliations();
        var event = randomEvent(person, loginEventType);
        var orgFeideDomain = event
            .getRequest()
            .getUserAttributes()
            .get(ORG_FEIDE_DOMAIN);
        var currentCustomer = fetchCustomerBasedOnFeideDomain(orgFeideDomain);
        var expectedTopLevelOrgUri = currentCustomer.getCristinId();

        handler.handleRequest(event, context);
        var actualTopOrgCristinId = getUpdatedClaimFromCognito(TOP_ORG_CRISTIN_ID);
        assertThat(actualTopOrgCristinId, is(equalTo(expectedTopLevelOrgUri.toString())));
    }

    @ParameterizedTest(name = "should store user's username in cognito user attributes when user has "
                              + "many active affiliations but logged in with Feide")
    @EnumSource(value = LoginEventType.class, names = {"FEIDE"}, mode = Mode.INCLUDE)
    void shouldStoreUsersUsernameWhenUserHasManyActiveAffiliationsAndLoggedInWithFeide(
        LoginEventType loginEventType) {
        var person = registeredPeople.personWithManyActiveAffiliations();
        var event = randomEvent(person, loginEventType);
        var orgFeideDomain = event
            .getRequest()
            .getUserAttributes()
            .get(ORG_FEIDE_DOMAIN);
        var currentCustomer = fetchCustomerBasedOnFeideDomain(orgFeideDomain);
        var personCristinIdentifier = UriWrapper.fromUri(registeredPeople.getCristinPersonId(person))
            .getLastPathElement();
        var customerCristinIdentifier = UriWrapper.fromUri(currentCustomer.getCristinId())
            .getLastPathElement();
        var expectedUsername = personCristinIdentifier + AT + customerCristinIdentifier;
        handler.handleRequest(event, context);
        var actualUsername = getUpdatedClaimFromCognito(NVA_USERNAME_CLAIM);
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @ParameterizedTest(name = "should store user's username in cognito user attributes when "
                              + "user has only one active affiliation")
    @EnumSource(LoginEventType.class)
    void shouldStoreUsersUsernameWhenUserHasOnlyOneActiveAffiliation(LoginEventType loginEventType) {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();

        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);

        var expectedUsername = scanAllUsers().stream()
            .collect(SingletonCollector.collect())
            .getUsername();
        var actualUsername = getUpdatedClaimFromCognito(NVA_USERNAME_CLAIM);

        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @ParameterizedTest(name = "should create role \"Creator\" when the role does not exist")
    @EnumSource(LoginEventType.class)
    void shouldCreateRoleCreatorWhenRoleDoesNotExist(LoginEventType loginEventType) throws NotFoundException {
        var person = registeredPeople.personWithActiveAndInactiveAffiliations();
        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);

        var actualRole = identityService.getRole(ROLE_FOR_USERS_WITH_ACTIVE_AFFILIATION);
        assertThat(actualRole.getRoleName(), is(equalTo(ROLE_FOR_USERS_WITH_ACTIVE_AFFILIATION.getRoleName())));
    }

    @ParameterizedTest(name = "should not fail when role \"Creator\" already exits")
    @EnumSource(LoginEventType.class)
    void shouldNotFailWhenUserRoleAlreadyExists(LoginEventType loginEventType)
        throws InvalidInputException, ConflictException, NotFoundException {
        identityService.addRole(ROLE_FOR_USERS_WITH_ACTIVE_AFFILIATION);
        var person = registeredPeople.personWithActiveAndInactiveAffiliations();
        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualRole = identityService.getRole(ROLE_FOR_USERS_WITH_ACTIVE_AFFILIATION);
        assertThat(actualRole.getRoleName(), is(equalTo(ROLE_FOR_USERS_WITH_ACTIVE_AFFILIATION.getRoleName())));
    }

    @ParameterizedTest(name = "should add role \"Creator\" to new user entries")
    @EnumSource(LoginEventType.class)
    void shouldAddRoleUserToNewUserEntries(LoginEventType loginEventType) {
        var person = registeredPeople.personWithActiveAndInactiveAffiliations();
        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var users = scanAllUsers();
        for (var user : users) {
            assertThatUserHasUserRoleAttached(user);
        }
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldAddUserAffiliationToNewUserEntryWhenUserEntryDoesNotPreexist(LoginEventType loginEventType) {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var user = scanAllUsers().stream().collect(SingletonCollector.collect());
        var organization = registeredPeople.getOrganizations(person)
            .stream().collect(SingletonCollector.collect());
        assertThat(user.getAffiliation(), is(equalTo(organization)));
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldAddUserAffiliationToExistingUserEntryWhenUserEntryPreexists(LoginEventType loginEventType)
        throws NotFoundException {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        var existingUser = createUsersForAffiliations(person, ONLY_ACTIVE)
            .stream().collect(SingletonCollector.collect());
        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var updateUser = identityService.getUser(existingUser);
        var affiliation = registeredPeople.getOrganizations(person)
            .stream().collect(SingletonCollector.collect());
        assertThat(updateUser.getAffiliation(), is(equalTo(affiliation)));
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldUpdateCognitoUserInfoDetailsWithCurrentUserAffiliation(LoginEventType loginEventType)
        throws NotFoundException {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        var existingUser = createUsersForAffiliations(person, ONLY_ACTIVE)
            .stream().collect(SingletonCollector.collect());
        var event = randomEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var updateUser = identityService.getUser(existingUser);
        var affiliation = registeredPeople.getOrganizations(person)
            .stream().collect(SingletonCollector.collect());
        var cognitoAttribute = getUpdatedClaimFromCognito(PERSON_AFFILIATION_CLAIM);
        assertThat(cognitoAttribute, is(equalTo(affiliation.toString())));
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldUpdateCognitoGroupsToIncludeCustomerIdWhenUserHasExactlyOneActiveAffiliation(
        LoginEventType loginEventType) {
        var person = registeredPeople.personWithExactlyOneActiveAffiliation();
        var event = randomEvent(person, loginEventType);
        var response = handler.handleRequest(event, context);
        var groups = extractAccessRights(response);
        var expectedCustomerId = registeredPeople.getTopLevelOrgsForPerson(person, ONLY_ACTIVE).stream()
            .map(attempt(id -> customerService.getCustomerByCristinId(id)))
            .map(Try::orElseThrow)
            .map(CustomerDto::getId)
            .collect(SingletonCollector.collect());

        var expectedCognitoGroup =
            "USER" + AT + expectedCustomerId.toString();

        assertThat(groups, hasItem(expectedCognitoGroup));
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldAllowPeopleWhoAreNotRegisteredInPersonRegistryToLoginButNotGiveThemAnyRole(
        LoginEventType loginEventType) {
        var person = registeredPeople.personThatIsNotRegisteredInPersonRegistry();
        var event = randomEvent(person, loginEventType);
        var response = handler.handleRequest(event, context);
        var accessRights = extractAccessRights(response);
        assertThat(accessRights, is((empty())));
    }

    private void assertThatUserHasUserRoleAttached(UserDto user) {
        var userRoles = user.getRoles().stream().map(RoleDto::getRoleName).collect(Collectors.toList());
        assertThat(userRoles, hasItem("Creator"));
    }

    private String getUpdatedClaimFromCognito(String attributeName) {
        return congitoClient.getAdminUpdateUserRequest().userAttributes().stream()
            .filter(attribute -> attributeName.equals(attribute.name()))
            .map(AttributeType::value)
            .collect(SingletonCollector.collect());
    }

    private List<String> createRoleStrings(UserDto user) {
        return user.getRoles().stream()
            .map(RoleDto::getRoleName)
            .map(rolename -> rolename + AT + user.getInstitution().toString())
            .collect(Collectors.toList());
    }

    private List<String> extractAccessRights(CognitoUserPoolPreTokenGenerationEvent response) {
        return Arrays.asList(
            response.getResponse().getClaimsOverrideDetails().getGroupOverrideDetails().getGroupsToOverride());
    }

    private CustomerDto fetchCustomerBasedOnFeideDomain(String customersFeideDomain) {
        return customerService.getCustomers().stream()
            .filter(customer -> customersFeideDomain.equals(customer.getFeideOrganizationDomain()))
            .collect(SingletonCollector.collectOrElse(null));
    }

    private String fetchCurrentCustomClaimForCongitoUserUpdate() {
        var request = congitoClient.getAdminUpdateUserRequest();
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
        var expectedTopLevelOrgId = registeredPeople.getTopLevelOrgsForPerson(person, ONLY_ACTIVE)
            .stream().collect(SingletonCollector.collect());
        assertThat(updatedUser.getInstitutionCristinId(), is(equalTo(expectedTopLevelOrgId)));
    }

    private UserDto legacyUserWithFeideIdentifierAsUsername(NationalIdentityNumber person,
                                                            String personsFeideIdentifier) {
        try {
            var preExistingUser = nvaDataGenerator.createUsers(person, ONLY_ACTIVE)
                .stream().collect(SingletonCollector.collect());
            preExistingUser.setUsername(personsFeideIdentifier);
            preExistingUser.setFeideIdentifier(NOT_EXISTING_VALUE_IN_LEGACY_ENTRIES);
            preExistingUser.setInstitutionCristinId(NOT_EXISTING_URI_IN_LEGACY_ENTRIES);
            preExistingUser.setCristinId(NOT_EXISTING_URI_IN_LEGACY_ENTRIES);
            identityService.addUser(preExistingUser);
            return preExistingUser;
        } catch (InvalidInputException | ConflictException e) {
            throw new RuntimeException(e);
        }
    }

    private String feideDomainOfUsersInstitution(NationalIdentityNumber person) throws NotFoundException {
        var topLeveOrg = registeredPeople.getTopLevelOrgsForPerson(person, ONLY_ACTIVE)
            .stream()
            .collect(SingletonCollector.collect());
        return customerService.getCustomerByCristinId(topLeveOrg).getFeideOrganizationDomain();
    }

    private UserDto fetchUserFromDatabase(URI cristinPersonId, URI customerCristinId) {
        return identityService.getUserByPersonCristinIdAndCustomerCristinId(cristinPersonId, customerCristinId);
    }

    private void assertThatUserIsSearchableByCristinCredentials(NationalIdentityNumber personLoggingIn,
                                                                List<UserDto> allUsers) {
        assertThatUsersCristinIsPersonsCristinId(personLoggingIn, allUsers);
        assertThatUsersInstitutionCristinIdIsCustomersCristinId(personLoggingIn, allUsers);
    }

    private void assertThatUsersInstitutionCristinIdIsCustomersCristinId(NationalIdentityNumber personLoggingIn,
                                                                         List<UserDto> allUsers) {
        var expectedCustomerCristinIds = constructExpectedCustomersFromMockData(personLoggingIn);
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

    private List<UserDto> createUsersForAffiliations(NationalIdentityNumber personLoggingIn, boolean includeInactive) {
        return nvaDataGenerator.createUsers(personLoggingIn, includeInactive)
            .stream()
            .map(attempt(user -> identityService.addUser(user)))
            .map(attempt -> attempt.map(user -> identityService.getUser(user)))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private List<String> createAccessRightsNvaVersion(UserDto user) {
        var customerId = attempt(() -> customerService.getCustomer(user.getInstitution()))
            .map(CustomerDto::getId)
            .map(URI::toString)
            .orElseThrow();

        return user.getAccessRights().stream()
            .map(accessRight -> accessRight + AT + customerId)
            .collect(Collectors.toList());
    }

    private List<String> createAccessRightsCristinIdVersion(UserDto user) {
        var customerCristinId =
            attempt(() -> customerService.getCustomer(user.getInstitution()).getCristinId()).orElseThrow();
        return user.getAccessRights().stream()
            .map(accessRight -> accessRight + AT + customerCristinId)
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

    private List<URI> constructExpectedCustomersFromMockData(NationalIdentityNumber personLoggingIn) {
        return registeredPeople.getTopLevelOrgsForPerson(personLoggingIn, ONLY_ACTIVE)
            .stream()
            .map(attempt(cristinId -> customerService.getCustomerByCristinId(cristinId)))
            .map(Try::orElseThrow)
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
            .withCallerContext(CallerContext.builder().withClientId(authServerMock.getClientId()).build())
            .build();
    }

    private CognitoUserPoolPreTokenGenerationEvent randomEventOfNonFeideUser(NationalIdentityNumber nin) {
        Map<String, String> userAttributes = Map.of(NIN_FON_NON_FEIDE_USERS, nin.toString());
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withUserName(randomString())
            .withRequest(Request.builder().withUserAttributes(userAttributes).build())
            .withCallerContext(CallerContext.builder().withClientId(authServerMock.getClientId()).build())
            .build();
    }

    private void setupIdentityService() {
        this.userAccessDynamoDbLocal = new LocalIdentityService() {
        };
        userAccessDynamoDbLocal.initializeTestDatabase();
        this.identityService = new IdentityServiceImpl(userAccessDynamoDbLocal.getDynamoDbClient());
    }

    private void setupCustomerService() {
        this.customerDynamoDbLocal = new LocalCustomerServiceDatabase();
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