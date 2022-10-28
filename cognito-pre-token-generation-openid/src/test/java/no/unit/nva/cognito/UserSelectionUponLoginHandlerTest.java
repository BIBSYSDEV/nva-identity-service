package no.unit.nva.cognito;

import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.AuthenticationInformation.COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR;
import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ROLES_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.cognito.CristinProxyMock.INACTIVE;
import static no.unit.nva.cognito.MockPersonRegistry.ACTIVE;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.NIN_FON_NON_FEIDE_USERS;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.ORG_FEIDE_DOMAIN;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.PERSON_REGISTRY_HOST;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.model.UserDto.AT;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.FakeCognito;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.cognito.MockPersonRegistry.EmploymentInformation;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

@WireMockTest(httpsEnabled = true)
class UserSelectionUponLoginHandlerTest {
    
    public static final int SINGLE_EXPECTED_USER = 1;
    
    private final Context context = new FakeContext();
    private UserSelectionUponLoginHandler handler;
    
    private DynamoDBCustomerService customerService;
    
    private IdentityService identityService;
    private FakeCognito cognitoClient;
    private MockPersonRegistry personRegistry;
    private LocalIdentityService identityServiceDb;
    private ImaginarySetup imaginarySetup;
    
    @BeforeEach
    public void init(WireMockRuntimeInfo wireMockRuntimeInfo) throws InvalidInputException, ConflictException {
        identityService = initializeIdentityService();
        customerService = initializeCustomerService();
        
        var wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
        var oauth2ServerMock = new NvaOAuth2ServerMock();
        String backendAccessToken = oauth2ServerMock.setupCognitoMockResponse();
        
        personRegistry = new MockPersonRegistry(backendAccessToken, wiremockUri);
        
        var cognitoCredentials = new CognitoCredentials(oauth2ServerMock::getClientId,
            oauth2ServerMock::getClientSecret, wiremockUri);
        
        var authorizedHttpClient = AuthorizedBackendClient.prepareWithCognitoCredentials(WiremockHttpClient.create(),
            cognitoCredentials);
        
        imaginarySetup = new ImaginarySetup(personRegistry, customerService, identityService);
        cognitoClient = new FakeCognito(randomString());
        var environment = setupEnvironment(wiremockUri);
        handler = new UserSelectionUponLoginHandler(environment, cognitoClient, authorizedHttpClient, customerService,
            identityService);
    }
    
    @AfterEach
    public void tearDown() {
        identityServiceDb.closeDB();
    }
    
    @ParameterizedTest(name = "Login event type: {0}")
    @DisplayName("should create user for the person's institution (top org) when person has not"
                 + "logged in before and has one active employment")
    @EnumSource(LoginEventType.class)
    void shouldCreateUserForPersonsTopOrganizationWhenPersonHasNotLoggedInBeforeAndHasOneActiveEmployment(
        LoginEventType loginEventType) {
        var personLoggingIn = imaginarySetup.personWithExactlyOneActiveEmployment();
        var event = newLoginEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);
        List<UserDto> allUsers = scanAllUsers();
        var actualUser = allUsers.get(0);
        assertThat(allUsers, hasSize(SINGLE_EXPECTED_USER));
        var expectedAffiliation = personRegistry.fetchTopOrgEmploymentInformation(personLoggingIn)
                                      .stream()
                                      .map(EmploymentInformation::getTopLevelOrg)
                                      .collect(SingletonCollector.collect());
        assertThat(actualUser.getInstitutionCristinId(), is(equalTo(expectedAffiliation)));
    }
    
    //TODO: is it possible to not have an active employment and be able to login through FEIDE and what should happen
    // then?
    @ParameterizedTest(name = "should not create user for the person's institution (top org) when person has not "
                              + "logged in before and has only inactive employment")
    @EnumSource(value = LoginEventType.class, names = {"NON_FEIDE"})
    void shouldNotCreateUserForPersonsTopOrganizationWhenPersonHasNotLoggedInBeforeAndHasOnlyInactiveEmployment(
        LoginEventType loginEventType) {
        
        var personLoggingIn = imaginarySetup.personWithExactlyOneInactiveEmployment();
        var event = newLoginEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);
        var actualUsers = scanAllUsers();
        assertThat(actualUsers, is(empty()));
    }
    
    @ParameterizedTest(name = "should not create user for institutions (top orgs) that the user has only inactive "
                              + "employments with "
                              + "when person has not logged in before "
                              + "and has active and inactive affiliations in different institutions")
    @EnumSource(LoginEventType.class)
    void shouldNotCreateUserForTopOrgsWithInactiveEmployments(LoginEventType loginEventType) {
        
        var personLoggingIn = imaginarySetup.personWithOneActiveAndOneInactiveEmploymentInDifferentTopLevelOrgs();
        var event = newLoginEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);
        var activeEmployments = fetchTopLevelOrgEmployments(personLoggingIn, ACTIVE);
        var inactiveEmployments = fetchTopLevelOrgEmployments(personLoggingIn, INACTIVE);
        var allUsers = scanAllUsers();
        var topLevelOrgsForCreatedUsers = allUsers.stream()
                                              .map(UserDto::getInstitutionCristinId)
                                              .collect(Collectors.toSet());
        assertThat(topLevelOrgsForCreatedUsers, containsInAnyOrder(activeEmployments.toArray(URI[]::new)));
        assertThat(inactiveEmployments, everyItem(not(in(topLevelOrgsForCreatedUsers))));
    }
    
    @ParameterizedTest(name = "should create user for institution (top org) when the user has both active and "
                              + "inactive employment with that institution")
    @EnumSource(LoginEventType.class)
    void shouldCreateUserForInstitutionWhenTheUserHasBothActiveAndInactiveEmploymentWithThatInstitution(
        LoginEventType loginEventType) {
    
        var personLoggingIn = imaginarySetup.personWithOneActiveAndOneInactiveEmploymentInSameTopLevelOrg();
        var event = newLoginEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);
        var activeEmployments = fetchTopLevelOrgEmployments(personLoggingIn, ACTIVE);
        var inactiveEmployments = fetchTopLevelOrgEmployments(personLoggingIn, INACTIVE);
        var allUsers = scanAllUsers();
        var topLevelOrgsForCreatedUsers = allUsers.stream()
                                              .map(UserDto::getInstitutionCristinId)
                                              .collect(Collectors.toSet());
        assertThat(allUsers, hasSize(SINGLE_EXPECTED_USER));
        assertThatEmployeeWithInactiveAndActiveEmploymentInSameTopLevelOrgGetsAUser(topLevelOrgsForCreatedUsers,
            activeEmployments, inactiveEmployments);
    }
    
    @ParameterizedTest(name = "should maintain legacy user names for users that have already logged in "
                              + "to avoid missing the reference between publications and users")
    @EnumSource(LoginEventType.class)
    void shouldMaintainUsernameInPreexistingUserEntriesForBothActiveAndInactiveEmployments(LoginEventType eventType) {
        var personLoggingIn = imaginarySetup.personWithOneActiveAndOneInactiveEmploymentInDifferentTopLevelOrgs();
        var preExistingUsers = createUsersForAllAffiliations(personLoggingIn);
        var expectedUsernames = preExistingUsers.stream().map(UserDto::getUsername).collect(Collectors.toList());
        var event = newLoginEvent(personLoggingIn, eventType);
        handler.handleRequest(event, context);
        var users = scanAllUsers();
        var actualUsernames = users.stream().map(UserDto::getUsername).collect(Collectors.toList());
        assertThat(actualUsernames, containsInAnyOrder(expectedUsernames.toArray(String[]::new)));
    }
    
    @ParameterizedTest(name = "should return access rights as user groups for user within the scope of a customer "
                              + "for user's active top orgs")
    @EnumSource(LoginEventType.class)
    void shouldReturnAccessRightsForUserConcatenatedWithCustomerNvaIdentifierForUsersActiveTopOrgs(
        LoginEventType eventType) throws ConflictException, NotFoundException, InvalidInputException {
        var personLoggingIn = imaginarySetup.personWithExactlyOneActiveEmployment();
        var existingUserInitiallyWithoutRoles = createUsersForAllAffiliations(personLoggingIn).stream()
                                                    .collect(SingletonCollector.collect());
        var assignedAccessRights = randomAccessRights();
        var role = persistRoleToDatabase(assignedAccessRights);
        assignExistingRoleToUser(existingUserInitiallyWithoutRoles, role);
        var event = newLoginEvent(personLoggingIn, eventType);
        var response = handler.handleRequest(event, context);
        assertThatResponseContainsAssignedAccessRights(existingUserInitiallyWithoutRoles, assignedAccessRights,
            response);
    
        assertThatAccessRightsArePersistedInCognitoEntry(existingUserInitiallyWithoutRoles, assignedAccessRights);
    }
    
    @ParameterizedTest(name = "should add customer id as current-customer-id when user logs in and has only one "
                              + "active employment")
    @EnumSource(LoginEventType.class)
    void shouldAddCustomerIdAsChosenCustomerIdWhenUserLogsInAndHasOnlyOneActiveEmployment(
        LoginEventType loginEventType) {
        var person = imaginarySetup.personWithExactlyOneActiveEmployment();
        var expectedCustomerId = imaginarySetup.fetchCustomersForPerson(person)
                                     .stream()
                                     .collect(SingletonCollector.collect())
                                     .getId();
    
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
    
        var actualCustomerId = fetchCurrentCustomClaimForCognitoUserUpdate();
        assertThat(actualCustomerId, is(equalTo(expectedCustomerId.toString())));
    }
    
    @ParameterizedTest(name = "should add Feide specified customer id as current customer id when user logs in "
                              + "with feide")
    @EnumSource(value = LoginEventType.class, names = {"FEIDE"}, mode = Mode.INCLUDE)
    void shouldAddFeideSpecifiedCustomerIdAsCurrentCustomerIdWhenUserLogsInWithFeide(LoginEventType loginEventType)
        throws NotFoundException {
        var person = imaginarySetup.personWithTwoActiveEmployments();
        var event = newLoginEvent(person, loginEventType);
        var customerFeideDomain = extractFeideDomainFromInputEvent(event);
        var expectedCustomerId = customerService.getCustomerByOrgDomain(customerFeideDomain).getId();
        handler.handleRequest(event, context);
        var actualCustomerId = fetchCurrentCustomClaimForCognitoUserUpdate();
        assertThat(actualCustomerId, is(equalTo(expectedCustomerId.toString())));
    }
    
    //TODO: This test is connected to issue https://unit.atlassian.net/browse/NP-9126.
    // Talk with frontend, tech-lead, team-leader, project manager to establish the current expectations.
    @ParameterizedTest(name = "should not update customerId when user has many affiliations and logs in with"
                              + "personal number")
    @EnumSource(value = LoginEventType.class, names = {"NON_FEIDE"}, mode = Mode.INCLUDE)
    void shouldNotUpdateCurrentCustomerIdWenUserHasManyAffilationsAndLogsInWithPersonalNumber(
        LoginEventType loginEventType) {
        var person = imaginarySetup.personWithTwoActiveEmployments();
        var event = newLoginEvent(person, loginEventType);
        
        handler.handleRequest(event, context);
        
        var actualCustomerId = fetchCurrentCustomClaimForCognitoUserUpdate();
        assertThat(actualCustomerId, is(nullValue()));
    }
    
    @ParameterizedTest(name = "should not assign access rights for active employment when institution (top-level "
                              + "org) is not a registered customer in NVA")
    @EnumSource(LoginEventType.class)
    void shouldNotAssignAccessRightsForActiveAffiliationsWhenTopLevelOrgIsNotARegisteredCustomerInNva(
        LoginEventType loginEventType) {
        var person = imaginarySetup.personWithExactlyOneActiveEmploymentInNonRegisteredTopLevelOrg();
        var event = newLoginEvent(person, loginEventType);
        var response = handler.handleRequest(event, context);
        var allUsers = scanAllUsers();
        assertThat(allUsers, is(empty()));
        var accessRights = extractAccessRights(response);
        assertThat(accessRights, is(empty()));
    }
    
    // The following scenario happens when a customer was deleted and instead of being restored by backup data,
    // it wes re-created. As a result, existing users will reference the correct Cristin Org entry, but the incorrect
    // (old) NVA Customer entry
    @ParameterizedTest(name = "should fail when user has inconsistent values for 'institution' (customerId) "
                              + "and  'institutionCristinId' (cristinCustomerId). ")
    @EnumSource(LoginEventType.class)
    void shouldFailWhenSelectedUserHasWrongCustomerId(LoginEventType loginEventType) throws NotFoundException {
        var person = imaginarySetup.personWithExactlyOneActiveEmployment();
        
        var existingUser = createUsersForAllAffiliations(person).stream().collect(SingletonCollector.collect());
        existingUser.setInstitution(randomUri());
        identityService.updateUser(existingUser);
        var event = newLoginEvent(person, loginEventType);
        var exception = assertThrows(IllegalStateException.class, () -> handler.handleRequest(event, context));
        assertThat(exception.getMessage(), containsString(COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR));
    }
    
    @ParameterizedTest(name = "should include all customers with active employments in the Cognito field "
                              + "custom:allowedCustomers")
    @EnumSource(LoginEventType.class)
    void shouldIncludeAllCustomersWithActiveEmploymentsInCognitoField(LoginEventType loginEventType) {
        var person = imaginarySetup.personWithTwoActiveEmployments();
        var expectedCustomerIds = imaginarySetup.fetchCustomersForPerson(person)
                                      .stream()
                                      .map(CustomerDto::getId)
                                      .collect(Collectors.toList());
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualAllowedCustomerIds = extractAllowedCustomersFromCongitoUpdateRequest();
        
        assertThat(actualAllowedCustomerIds, containsInAnyOrder(expectedCustomerIds.toArray(URI[]::new)));
    }
    
    @ParameterizedTest(name = "should not include customers with inactive employments in the Cognito field "
                              + "custom:allowedCustomers")
    @EnumSource(LoginEventType.class)
    void shouldNotIncludeCustomersWithInactiveEmploymentsInCognitoField(LoginEventType loginEventType) {
        var person = imaginarySetup.personWithOneActiveAndOneInactiveEmploymentInDifferentTopLevelOrgs();
        var expectedCustomerIds = fetchCustomersWithActiveEmploymentsForPerson(person);
        assertThat(expectedCustomerIds, hasSize(1));
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualAllowedCustomerIds = extractAllowedCustomersFromCongitoUpdateRequest();
        assertThat(actualAllowedCustomerIds, containsInAnyOrder(expectedCustomerIds.toArray(URI[]::new)));
    }
    
    @ParameterizedTest(name = "should store all user's roles for each active top level affiliation in cognito "
                              + "user attributes")
    @EnumSource(LoginEventType.class)
    void shouldStoreAllUserRolesForEachActiveTopLevelAffiliationInCognitoUserAttributes(LoginEventType loginEventType) {
        var person = imaginarySetup.personWithTwoActiveEmployments();
        var usersWithRoles = createUsersWithRolesForPerson(person);
        
        var expectedRoles = usersWithRoles.stream()
                                .flatMap(this::createScopedRoles)
                                .collect(Collectors.toSet());
        
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualRoles = cognitoClient.getAdminUpdateUserRequest().userAttributes().stream()
                              .filter(attribute -> attribute.name().equals(ROLES_CLAIM))
                              .map(AttributeType::value)
                              .map(str -> str.split(","))
                              .flatMap(Arrays::stream)
                              .collect(Collectors.toList());
        assertThat(actualRoles, containsInAnyOrder(expectedRoles.toArray(String[]::new)));
    }
    
    @ParameterizedTest(name = "should store person's cristin Id in cognito user attributes")
    @EnumSource(LoginEventType.class)
    void shouldStorePersonCristinIdInCognitoUserAttributes(LoginEventType loginEventType) {
        var person = imaginarySetup.personWithExactlyOneActiveEmployment();
        var expectedCristinId = imaginarySetup.getPersonRegistryEntry(person).getCristinId();
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualCristinPersonId = extractClaimFromCognitoUpdateRequest(PERSON_CRISTIN_ID_CLAIM);
        
        assertThat(URI.create(actualCristinPersonId), is(equalTo(expectedCristinId)));
    }
    
    @ParameterizedTest(name = "should store user's top-level-org affiliation in cognito user attributes when "
                              + "user has only one active affiliation")
    @EnumSource(LoginEventType.class)
    void shouldStoreUsersTopLevelAffiliationWhenUserHasOnlyOneActiveAffiliation(LoginEventType loginEventType) {
        var person = imaginarySetup.personWithExactlyOneActiveEmployment();
        var personTopLevelOrgAffiliation = imaginarySetup.fetchTopOrgEmploymentInformation(person)
                                               .stream()
                                               .map(EmploymentInformation::getTopLevelOrg)
                                               .collect(SingletonCollector.collect());
        
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualTopOrgCristinId = extractClaimFromCognitoUpdateRequest(TOP_ORG_CRISTIN_ID);
        
        assertThat(URI.create(actualTopOrgCristinId), is(equalTo(personTopLevelOrgAffiliation)));
    }
    
    @ParameterizedTest(name = "should store user's top-level-org affiliation in cognito user attributes when user has"
                              + " many active affiliations but logged in with Feide")
    @EnumSource(value = LoginEventType.class, names = {"FEIDE"}, mode = Mode.INCLUDE)
    void shouldStoreUsersTopLevelAffiliationWhenUserHasManyActiveAffiliationsAndLoggedInWithFeide(
        LoginEventType loginEventType) throws NotFoundException {
        var person = imaginarySetup.personWithTwoActiveEmployments();
        var event = newLoginEvent(person, loginEventType);
        var orgFeideDomain = extractFeideDomainFromInputEvent(event);
        var currentCustomer = customerService.getCustomerByOrgDomain(orgFeideDomain);
        var expectedTopLevelOrgUri = currentCustomer.getCristinId();
        
        handler.handleRequest(event, context);
        var actualTopOrgCristinId = extractClaimFromCognitoUpdateRequest(TOP_ORG_CRISTIN_ID);
        assertThat(actualTopOrgCristinId, is(equalTo(expectedTopLevelOrgUri.toString())));
    }
    
    private static HashSet<AccessRight> randomAccessRights() {
        return new HashSet<>(List.of(randomElement(AccessRight.values()), randomElement(AccessRight.values())));
    }
    
    private static void assertThatResponseContainsAssignedAccessRights(UserDto existingUser,
                                                                       Set<AccessRight> assignedAccessRights,
                                                                       CognitoUserPoolPreTokenGenerationEvent response) {
        var groups = response.getResponse().getClaimsOverrideDetails().getGroupOverrideDetails().getGroupsToOverride();
        var groupsList = Arrays.asList(groups);
        var expectedAccessRight = constructExpectedAccessRights(existingUser, assignedAccessRights);
        assertThat(groupsList, hasItems(expectedAccessRight.toArray(String[]::new)));
    }
    
    @ParameterizedTest(name = "should store user's username in cognito user attributes when user has "
                              + "many active affiliations but logged in with Feide")
    @EnumSource(value = LoginEventType.class, names = {"FEIDE"}, mode = Mode.INCLUDE)
    void shouldStoreUsersUsernameWhenUserHasManyActiveAffiliationsAndLoggedInWithFeide(
        LoginEventType loginEventType) throws NotFoundException {
        var person = imaginarySetup.personWithTwoActiveEmployments();
        var event = newLoginEvent(person, loginEventType);
        var orgFeideDomain = extractFeideDomainFromInputEvent(event);
        var currentCustomer = customerService.getCustomerByOrgDomain(orgFeideDomain);
        var expectedUsername = constructExpectedUsername(person, currentCustomer);
        
        handler.handleRequest(event, context);
        var actualUsername = extractClaimFromCognitoUpdateRequest(NVA_USERNAME_CLAIM);
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }
    
    @ParameterizedTest(name = "should store user's username in cognito user attributes when "
                              + "user has only one active affiliation")
    @EnumSource(LoginEventType.class)
    void shouldStoreUsersUsernameWhenUserHasOnlyOneActiveAffiliation(LoginEventType loginEventType) {
        var person = imaginarySetup.personWithExactlyOneActiveEmployment();
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var currentCustomer = customerService.getCustomers().stream().collect(SingletonCollector.collect());
        var expectedUsername = constructExpectedUsername(person, currentCustomer);
        var actualUsername = extractClaimFromCognitoUpdateRequest(NVA_USERNAME_CLAIM);
        
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }
    
    @ParameterizedTest(name =
                           "should add role (\"Creator\") that distinguishes between logged in people with active "
                           + "employments "
                           + "and logged in people without active employments  to new user entries")
    @EnumSource(LoginEventType.class)
    void shouldAddRoleCreatorToNewUserEntries(LoginEventType loginEventType) {
        var person = imaginarySetup.personWithExactlyOneActiveEmployment();
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var users = scanAllUsers();
        for (var user : users) {
            var rolenames = user.getRoles().stream().map(RoleDto::getRoleName).collect(Collectors.toSet());
            assertThat(rolenames, hasItem("Creator"));
        }
    }
    
    private static Map<String, String> setupUserAttributesForFeideLogin(NationalIdentityNumber nin,
                                                                        String feideDomain) {
        var attributes = new ConcurrentHashMap<String, String>();
        attributes.put(NIN_FOR_FEIDE_USERS, nin.getNin());
        if (nonNull(feideDomain)) {
            attributes.put(ORG_FEIDE_DOMAIN, feideDomain);
        }
        return attributes;
    }
    
    private static String extractFeideDomainFromInputEvent(CognitoUserPoolPreTokenGenerationEvent event) {
        return event.getRequest().getUserAttributes().get(ORG_FEIDE_DOMAIN);
    }
    
    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldAddUserAffiliationToNewUserEntryWhenUserEntryDoesNotPreexist(LoginEventType loginEventType) {
        var person = imaginarySetup.personWithExactlyOneActiveEmployment();
        var employment = imaginarySetup.getPersonRegistryEntry(person).getAffiliations()
                             .stream()
                             .map(CristinAffiliation::getOrganizationUri)
                             .collect(SingletonCollector.collect());
        
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var user = scanAllUsers().stream().collect(SingletonCollector.collect());
        assertThat(user.getAffiliation(), is(equalTo(employment)));
    }
    
    private String fetchFeideDomainFromRandomCustomerWithActiveEmployment(NationalIdentityNumber nin) {
        return imaginarySetup.fetchCustomersForPerson(nin)
                   .stream()
                   .findAny()
                   .map(CustomerDto::getFeideOrganizationDomain)
                   .orElse(null);
    }
    
    private void assertThatAccessRightsArePersistedInCognitoEntry(UserDto existingUserInitiallyWithoutRoles,
                                                                  HashSet<AccessRight> assignedAccessRights) {
        var accessRightsPersistedInCognito = extractAccessRightFromCognitoEntry();
        var expectedAccessRights = constructExpectedAccessRights(existingUserInitiallyWithoutRoles,
            assignedAccessRights);
        for (var expectedAccessRight : expectedAccessRights) {
            assertThat(accessRightsPersistedInCognito, containsString(expectedAccessRight));
        }
    }
    
    private static List<String> constructExpectedAccessRights(UserDto existingUserInitiallyWithoutRoles,
                                                              Set<AccessRight> assignedAccessRights) {
        return assignedAccessRights.stream()
                   .map(accessRight -> accessRight + AT + existingUserInitiallyWithoutRoles.getInstitution().toString())
                   .collect(Collectors.toList());
    }
    
    private static void assertThatEmployeeWithInactiveAndActiveEmploymentInSameTopLevelOrgGetsAUser(
        Set<URI> topLevelOrgsForCreatedUsers, Set<URI> activeEmployments, Set<URI> inactiveEmployments) {
        assertThat(topLevelOrgsForCreatedUsers, containsInAnyOrder(activeEmployments.toArray(URI[]::new)));
        assertThat(topLevelOrgsForCreatedUsers, containsInAnyOrder(inactiveEmployments.toArray(URI[]::new)));
    }
    
    private void assignExistingRoleToUser(UserDto existingUser, RoleDto role) throws NotFoundException {
        existingUser.setRoles(Set.of(role));
        identityService.updateUser(existingUser);
    }
    
    private static Environment setupEnvironment(URI wiremockUri) {
        var environment = mock(Environment.class);
        var personRegistryHostUri = UriWrapper.fromUri(wiremockUri).addChild("cristin").toString();
        when(environment.readEnv(PERSON_REGISTRY_HOST)).thenReturn(personRegistryHostUri);
        return environment;
    }
    
    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldAddUserEmploymentMentionedInPersonRegistryToExistingUserDatabaseEntryWhenUserEntryPreexists(
        LoginEventType loginEventType)
        throws NotFoundException {
        var person = imaginarySetup.personWithExactlyOneActiveEmployment();
        var existingUser = createUsersForAllAffiliations(person)
                               .stream().collect(SingletonCollector.collect());
        removeEmployment(existingUser);
        var userBeforeLogIn = identityService.getUser(existingUser);
        assertThat(userBeforeLogIn.getAffiliation(), is(nullValue()));
        
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var userAfterLogin = identityService.getUser(existingUser);
        var affiliation = imaginarySetup.getPersonRegistryEntry(person)
                              .getAffiliations()
                              .stream()
                              .map(CristinAffiliation::getOrganizationUri)
                              .collect(SingletonCollector.collect());
        
        assertThat(userAfterLogin.getAffiliation(), is(equalTo(affiliation)));
    }
    
    private static CognitoUserPoolPreTokenGenerationEvent nonFeideLogin(NationalIdentityNumber nin) {
        var request = Request.builder().withUserAttributes(Map.of(NIN_FON_NON_FEIDE_USERS, nin.getNin())).build();
        var loginEvent = new CognitoUserPoolPreTokenGenerationEvent();
        loginEvent.setRequest(request);
        return loginEvent;
    }
    
    private Set<URI> fetchTopLevelOrgEmployments(NationalIdentityNumber personLoggingIn, boolean active) {
        return imaginarySetup.fetchTopOrgEmploymentInformation(personLoggingIn)
                   .stream()
                   .filter(employmentInformation -> employmentInformation.isActive() == active)
                   .map(EmploymentInformation::getTopLevelOrg)
                   .collect(Collectors.toSet());
    }
    
    private DynamoDBCustomerService initializeCustomerService() {
        var localDatabase = new LocalCustomerServiceDatabase();
        localDatabase.setupDatabase();
        return new DynamoDBCustomerService(localDatabase.getDynamoClient());
    }
    
    private IdentityService initializeIdentityService() {
        identityServiceDb = new LocalIdentityService();
        var client = identityServiceDb.initializeTestDatabase();
        return IdentityService.defaultIdentityService(client);
    }
    
    private List<UserDto> scanAllUsers() {
        var request = new ScanDatabaseRequestV2(randomString(), 100, null);
        return identityService.fetchOnePageOfUsers(request).getRetrievedUsers();
    }
    
    private CognitoUserPoolPreTokenGenerationEvent feideLogin(NationalIdentityNumber nin) {
        var feideDomain = fetchFeideDomainFromRandomCustomerWithActiveEmployment(nin);
        var request = Request.builder().withUserAttributes(setupUserAttributesForFeideLogin(nin, feideDomain)).build();
        var loginEvent = new CognitoUserPoolPreTokenGenerationEvent();
        loginEvent.setRequest(request);
        return loginEvent;
    }
    
    private String fetchCurrentCustomClaimForCognitoUserUpdate() {
        var request = fetchCognitoEntryUpdateRequestSentByHandler();
        return request.userAttributes()
                   .stream()
                   .filter(a -> a.name().equals(CURRENT_CUSTOMER_CLAIM))
                   .map(AttributeType::value)
                   .collect(SingletonCollector.collectOrElse(null));
    }
    
    private String extractAccessRightFromCognitoEntry() {
        return fetchCognitoEntryUpdateRequestSentByHandler().userAttributes()
                   .stream()
                   .filter(userAttribute -> ACCESS_RIGHTS_CLAIM.equals(userAttribute.name()))
                   .map(AttributeType::value)
                   .collect(SingletonCollector.collect());
    }
    
    private AdminUpdateUserAttributesRequest fetchCognitoEntryUpdateRequestSentByHandler() {
        return cognitoClient.getAdminUpdateUserRequest();
    }
    
    private RoleDto persistRoleToDatabase(Collection<AccessRight> accessRights)
        throws InvalidInputException, ConflictException, NotFoundException {
        var role = RoleDto.newBuilder().withRoleName(randomString()).withAccessRights(accessRights).build();
        persistRole(role);
        return identityService.getRole(role);
    }
    
    private UserDto createUserForAffiliation(CristinPersonResponse person, CristinAffiliation affiliation) {
        var topLevelOrgCristinId = imaginarySetup.getTopLevelOrgForNonTopLevelOrg(affiliation.getOrganizationUri());
        var customerId = attempt(() -> customerService.getCustomerByCristinId(topLevelOrgCristinId)).map(
            CustomerDto::getId).orElseThrow();
        var user = UserDto.newBuilder()
                       .withAffiliation(affiliation.getOrganizationUri())
                       .withCristinId(person.getCristinId())
                       .withUsername(randomString())
                       .withFamilyName(randomString())
                       .withGivenName(randomString())
                       .withFeideIdentifier(randomString())
                       .withInstitution(customerId)
                       .withInstitutionCristinId(topLevelOrgCristinId)
                       .build();
        return attempt(() -> identityService.addUser(user)).orElseThrow();
    }
    
    private CognitoUserPoolPreTokenGenerationEvent newLoginEvent(NationalIdentityNumber person,
                                                                 LoginEventType loginEventType) {
        return LoginEventType.FEIDE.equals(loginEventType) ? feideLogin(person) : nonFeideLogin(person);
    }
    
    private List<UserDto> createUsersWithRolesForPerson(NationalIdentityNumber person) {
        return createUsersForAllAffiliations(person)
                   .stream()
                   .map(attempt(user -> addRoleToUser(user, persistRandomRole())))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }
    
    private Stream<String> createScopedRoles(UserDto user) {
        var customerId = user.getInstitution();
        return user.getRoles().stream()
                   .map(RoleDto::getRoleName)
                   .map(rolename -> String.join(AT, rolename, customerId.toString()));
    }
    
    private UserDto addRoleToUser(UserDto user, RoleDto persistRandomRole) throws NotFoundException {
        user.setRoles(Set.of(persistRandomRole));
        identityService.updateUser(user);
        return identityService.getUser(user);
    }
    
    private RoleDto persistRandomRole() {
        var newRole = RoleDto.newBuilder()
                          .withRoleName(randomString())
                          .withAccessRights(randomAccessRights())
                          .build();
        attempt(() -> persistRole(newRole)).orElseThrow();
        return attempt(() -> identityService.getRole(newRole)).orElseThrow();
    }
    
    private Void persistRole(RoleDto newRole) throws ConflictException, InvalidInputException {
        identityService.addRole(newRole);
        return null;
    }
    
    private List<URI> fetchCustomersWithActiveEmploymentsForPerson(NationalIdentityNumber person) {
        return imaginarySetup.fetchTopOrgEmploymentInformation(person)
                   .stream()
                   .filter(EmploymentInformation::isActive)
                   .map(EmploymentInformation::getTopLevelOrg)
                   .map(attempt(cristinId -> customerService.getCustomerByCristinId(cristinId)))
                   .map(Try::orElseThrow)
                   .map(CustomerDto::getId)
                   .collect(Collectors.toList());
    }
    
    private List<URI> extractAllowedCustomersFromCongitoUpdateRequest() {
        return cognitoClient.getAdminUpdateUserRequest()
                   .userAttributes().stream()
                   .filter(attribute -> attribute.name().equals(ALLOWED_CUSTOMER_CLAIM))
                   .map(AttributeType::value)
                   .map(concatenatedUris -> concatenatedUris.split(","))
                   .flatMap(Arrays::stream)
                   .map(URI::create)
                   .collect(Collectors.toList());
    }
    
    private String extractClaimFromCognitoUpdateRequest(String claimName) {
        return cognitoClient.getAdminUpdateUserRequest()
                   .userAttributes().stream()
                   .filter(a -> a.name().equals(claimName))
                   .map(AttributeType::value)
                   .collect(SingletonCollector.collect());
    }
    
    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldUpdateCognitoUserInfoDetailsWithCurrentUserAffiliation(LoginEventType loginEventType) {
        var person = imaginarySetup.personWithExactlyOneActiveEmployment();
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var expectedAffiliation = imaginarySetup.getPersonRegistryEntry(person)
                                      .getAffiliations()
                                      .stream()
                                      .map(CristinAffiliation::getOrganizationUri)
                                      .collect(SingletonCollector.collect());
        var cognitoAttribute = extractClaimFromCognitoUpdateRequest(PERSON_AFFILIATION_CLAIM);
        assertThat(cognitoAttribute, is(equalTo(expectedAffiliation.toString())));
    }
    
    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldAllowPeopleWhoAreNotRegisteredInPersonRegistryToLoginButNotGiveThemAnyRole(
        LoginEventType loginEventType) {
        var person = imaginarySetup.personThatIsNotRegisteredInPersonRegistry();
        var event = newLoginEvent(person, loginEventType);
        var response = handler.handleRequest(event, context);
        var accessRights = extractAccessRights(response);
        assertThat(accessRights, is((empty())));
    }
    
    private List<UserDto> createUsersForAllAffiliations(NationalIdentityNumber personLoggingIn) {
        var person = imaginarySetup.getPersonRegistryEntry(personLoggingIn);
        var affiliations = person.getAffiliations();
        return affiliations.stream()
                   .map(affiliation -> createUserForAffiliation(person, affiliation))
                   .collect(Collectors.toList());
    }
    
    private String constructExpectedUsername(NationalIdentityNumber person, CustomerDto currentCustomer) {
        String personIdentifier = imaginarySetup.getPersonRegistryEntry(person)
                                      .getPersonsCristinIdentifier()
                                      .getValue();
        String topOrgCristinIdentifier = UriWrapper.fromUri(currentCustomer.getCristinId()).getLastPathElement();
        return String.join(AT, personIdentifier, topOrgCristinIdentifier);
    }
    
    private void removeEmployment(UserDto existingUser) throws NotFoundException {
        existingUser.setAffiliation(null);
        identityService.updateUser(existingUser);
    }
    
    private List<String> extractAccessRights(CognitoUserPoolPreTokenGenerationEvent response) {
        return Arrays.asList(
            response.getResponse().getClaimsOverrideDetails().getGroupOverrideDetails().getGroupsToOverride());
    }
}