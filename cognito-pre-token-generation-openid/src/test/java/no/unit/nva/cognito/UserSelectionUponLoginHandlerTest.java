package no.unit.nva.cognito;

import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.EMPTY_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ROLES_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.NIN_FOR_NON_FEIDE_USERS;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.ORG_FEIDE_DOMAIN;
import static no.unit.nva.database.IdentityService.Constants.ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.model.UserDto.AT;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
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
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.userceation.testing.cristin.AuthenticationScenarios;
import no.unit.nva.useraccessservice.userceation.testing.cristin.MockPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
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
    public static final boolean ACTIVE = true;
    public static final boolean INACTIVE = false;

    private final Context context = new FakeContext();
    private UserSelectionUponLoginHandler handler;

    private DynamoDBCustomerService customerService;

    private IdentityService identityService;
    private FakeCognito cognitoClient;
    private MockPersonRegistry mockPersonRegistry;
    private LocalIdentityService identityServiceDb;
    private LocalCustomerServiceDatabase customerServiceDatabase;
    private AuthenticationScenarios scenarios;
    private final FakeSecretsManagerClient secretsManagerClient = new FakeSecretsManagerClient();

    @BeforeEach
    public void init(WireMockRuntimeInfo wireMockRuntimeInfo) throws InvalidInputException, ConflictException {
        identityService = initializeIdentityService();
        customerService = initializeCustomerService();

        var cristinUsername = randomString();
        var cristinPassword = randomString();
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_USERNAME_SECRET_KEY, cristinUsername);
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_PASSWORD_SECRET_KEY, cristinPassword);

        var wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
        mockPersonRegistry = new MockPersonRegistry(cristinUsername, cristinPassword, wiremockUri);

        scenarios = new AuthenticationScenarios(mockPersonRegistry, customerService, identityService);
        cognitoClient = new FakeCognito(randomString());

        var httpClient = WiremockHttpClient.create();
        handler = new UserSelectionUponLoginHandler(cognitoClient, customerService, identityService,
                                                    CristinPersonRegistry.customPersonRegistry(
                                                        httpClient,
                                                        wiremockUri,
                                                        ServiceConstants.API_DOMAIN,
                                                        new SecretsReader(secretsManagerClient)));
    }

    @AfterEach
    public void tearDown() {
        identityServiceDb.closeDB();
        customerServiceDatabase.deleteDatabase();
    }

    @ParameterizedTest(name = "Login event type: {0}")
    @DisplayName("should create user for the person's institution (top org) when person has not "
                 + "logged in before and has one active employment")
    @EnumSource(LoginEventType.class)
    void shouldCreateUserForPersonsTopOrganizationWhenPersonHasNotLoggedInBeforeAndHasOneActiveEmployment(
        LoginEventType loginEventType) {
        var personLoggingIn = scenarios.personWithExactlyOneActiveEmployment();
        var event = newLoginEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);
        List<UserDto> allUsers = scanAllUsers();
        var actualUser = allUsers.get(0);
        assertThat(allUsers, hasSize(SINGLE_EXPECTED_USER));
        var expectedAffiliation = mockPersonRegistry.getInstitutionUnitCristinUris(personLoggingIn)
                                      .stream()
                                      .collect(SingletonCollector.collect());
        assertThat(actualUser.getInstitutionCristinId(), is(equalTo(expectedAffiliation)));
    }

    // TODO: is it possible to not have an active employment and be able to login through FEIDE and what should
    // happen then?
    @ParameterizedTest(name = "should not create user for the person's institution (top org) when person has not "
                              + "logged in before and has only inactive employment")
    @EnumSource(value = LoginEventType.class, names = {"NON_FEIDE"})
    void shouldNotCreateUserForPersonsTopOrganizationWhenPersonHasNotLoggedInBeforeAndHasOnlyInactiveEmployment(
        LoginEventType loginEventType) {

        var personLoggingIn = scenarios.personWithExactlyOneInactiveEmployment();
        var event = newLoginEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);
        var actualUsers = scanAllUsers();
        assertThat(actualUsers, is(empty()));
    }

    @ParameterizedTest(name = "should not create user for institutions (top orgs) that the user has only inactive "
                              + "employments with when person has not logged in before and has active and inactive "
                              + "affiliations in different institutions")
    @EnumSource(LoginEventType.class)
    void shouldNotCreateUserForTopOrgsWithInactiveEmployments(LoginEventType loginEventType) {

        var personLoggingIn =
            scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();
        var event = newLoginEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);

        var activeEmployments
            = scenarios.getCristinUriForInstitutionAffiliations(personLoggingIn, ACTIVE);
        var inactiveEmployments
            = scenarios.getCristinUriForInstitutionAffiliations(personLoggingIn, INACTIVE);
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

        var personLoggingIn = scenarios
                                  .personWithOneActiveAndOneInactiveEmploymentInSameInstitution();
        var event = newLoginEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);
        var activeEmployments
            = scenarios.getCristinUriForInstitutionAffiliations(personLoggingIn, ACTIVE);
        var inactiveEmployments
            = scenarios.getCristinUriForInstitutionAffiliations(personLoggingIn, INACTIVE);
        var allUsers = scanAllUsers();
        var topLevelOrgsForCreatedUsers = allUsers.stream()
                                              .map(UserDto::getInstitutionCristinId)
                                              .collect(Collectors.toSet());
        assertThat(allUsers, hasSize(SINGLE_EXPECTED_USER));
        assertThatEmployeeWithInactiveAndActiveEmploymentInSameTopLevelOrgGetsAUser(topLevelOrgsForCreatedUsers,
                                                                                    activeEmployments,
                                                                                    inactiveEmployments);
    }

    @ParameterizedTest(name = "should maintain legacy user names for users that have already logged in "
                              + "to avoid missing the reference between publications and users")
    @EnumSource(LoginEventType.class)
    void shouldMaintainUsernameInPreexistingUserEntriesForBothActiveAndInactiveEmployments(LoginEventType eventType) {
        var personLoggingIn =
            scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();
        var preExistingUsers = scenarios.createUsersForAllActiveAffiliations(personLoggingIn, identityService);
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
        var personLoggingIn = scenarios.personWithExactlyOneActiveEmployment();
        var existingUserInitiallyWithoutRoles
            = scenarios.createUsersForAllActiveAffiliations(personLoggingIn, identityService).stream()
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

    @ParameterizedTest(name = "should add customer id as custom:customerId claim when user logs in and has only one "
                              + "active employment")
    @EnumSource(LoginEventType.class)
    void shouldAddCustomerIdAsChosenCustomerIdWhenUserLogsInAndHasOnlyOneActiveEmployment(
        LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var expectedCustomerId = scenarios.fetchCustomersForPerson(person)
                                     .stream()
                                     .collect(SingletonCollector.collect())
                                     .getId();

        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);

        var actualCustomerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
        assertThat(actualCustomerId, is(equalTo(expectedCustomerId.toString())));
    }

    @ParameterizedTest(name = "should add Feide specified customer id as current customer id when user logs in "
                              + "with feide")
    @EnumSource(value = LoginEventType.class, names = {"FEIDE"}, mode = Mode.INCLUDE)
    void shouldAddFeideSpecifiedCustomerIdAsCurrentCustomerIdWhenUserLogsInWithFeide(LoginEventType
                                                                                         loginEventType)
        throws NotFoundException {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions();
        var event = newLoginEvent(person, loginEventType);
        var customerFeideDomain = extractFeideDomainFromInputEvent(event);
        var expectedCustomerId = customerService.getCustomerByOrgDomain(customerFeideDomain).getId();
        handler.handleRequest(event, context);
        var actualCustomerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
        assertThat(actualCustomerId, is(equalTo(expectedCustomerId.toString())));
    }

    @ParameterizedTest(name = "should clear customer selection claims when user has many affiliations and logs in with"
                              + "personal number")
    @EnumSource(value = LoginEventType.class, names = {"NON_FEIDE"}, mode = Mode.INCLUDE)
    void shouldClearCustomerSelectionClaimsIdWenUserHasManyAffiliationsAndLogsInWithPersonalNumber(
        LoginEventType loginEventType) {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions();
        var event = newLoginEvent(person, loginEventType);

        handler.handleRequest(event, context);

        var customerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
        var topOrgCristinId = extractClaimFromCognitoUpdateRequest(TOP_ORG_CRISTIN_ID);
        var nvaUsername = extractClaimFromCognitoUpdateRequest(NVA_USERNAME_CLAIM);
        var personAffiliation = extractClaimFromCognitoUpdateRequest(PERSON_AFFILIATION_CLAIM);

        assertThat(customerId, is(equalTo(EMPTY_CLAIM)));
        assertThat(topOrgCristinId, is(equalTo(EMPTY_CLAIM)));
        assertThat(nvaUsername, is(equalTo(EMPTY_CLAIM)));
        assertThat(personAffiliation, is(equalTo(EMPTY_CLAIM)));
    }

    @ParameterizedTest(name = "should not assign access rights for active employment when institution (top-level org) "
                              + "is not a registered customer in NVA")
    @EnumSource(LoginEventType.class)
    void shouldNotAssignAccessRightsForActiveAffiliationsWhenTopLevelOrgIsNotARegisteredCustomerInNva(
        LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmploymentInNonCustomer();
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
        var person = scenarios.personWithExactlyOneActiveEmployment();

        var existingUser = scenarios.createUsersForAllActiveAffiliations(person, identityService).stream()
                               .collect(SingletonCollector.collect());
        existingUser.setInstitution(randomUri());
        identityService.updateUser(existingUser);
        var event = newLoginEvent(person, loginEventType);
        var exception = assertThrows(IllegalStateException.class, () -> handler.handleRequest(event, context));
        assertThat(exception.getMessage(), containsString(COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR));
    }

    @ParameterizedTest(name = "should include all customers with active employments in the Cognito field "
                              + "custom:allowedCustomers")
    @EnumSource(value = LoginEventType.class, names = "NON_FEIDE", mode = Mode.INCLUDE)
    void shouldIncludeAllCustomersWithActiveEmploymentsInCognitoFieldAllowedCustomersWhenLoggingInWithNonFeide(
        LoginEventType loginEventType) {

        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions();
        var expectedCustomerIds = scenarios.fetchCustomersForPerson(person)
                                      .stream()
                                      .map(CustomerDto::getId)
                                      .collect(Collectors.toList());
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualAllowedCustomerIds = extractAllowedCustomersFromCongitoUpdateRequest();

        assertThat(actualAllowedCustomerIds, containsInAnyOrder(expectedCustomerIds.toArray(URI[]::new)));
    }

    @ParameterizedTest(name = "should include only customer from feide in the Cognito field "
                              + "custom:allowedCustomers")
    @EnumSource(value = LoginEventType.class, names = "FEIDE", mode = Mode.INCLUDE)
    void shouldIncludeOnlyFeideCustomerInCognitoFieldAllowedCustomersWhenLoggingInWithFeide(
        LoginEventType loginEventType) throws NotFoundException {

        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions();
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var orgFeideDomain = extractFeideDomainFromInputEvent(event);
        var currentCustomer = customerService.getCustomerByOrgDomain(orgFeideDomain);
        var actualAllowedCustomerIds = extractAllowedCustomersFromCongitoUpdateRequest();

        assertThat(actualAllowedCustomerIds, containsInAnyOrder(currentCustomer.getId()));
    }

    @ParameterizedTest(name = "should not include customers with inactive employments in the Cognito field "
                              + "custom:allowedCustomers")
    @EnumSource(LoginEventType.class)
    void shouldNotIncludeCustomersWithInactiveEmploymentsInCognitoField(LoginEventType loginEventType) {
        var person = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();
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
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions();
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
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var expectedCristinId = scenarios.getCristinIdForPerson(person);
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualCristinPersonId = extractClaimFromCognitoUpdateRequest(PERSON_CRISTIN_ID_CLAIM);

        assertThat(URI.create(actualCristinPersonId), is(equalTo(expectedCristinId)));
    }

    @ParameterizedTest(name = "should store user's top-level-org affiliation in cognito user attributes when "
                              + "user has only one active affiliation")
    @EnumSource(LoginEventType.class)
    void shouldStoreUsersTopLevelAffiliationWhenUserHasOnlyOneActiveAffiliation(LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var personTopLevelOrgAffiliation
            = scenarios.getCristinUriForInstitutionAffiliations(person).stream()
                  .collect(SingletonCollector.tryCollect()).orElseThrow();

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
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions();
        var event = newLoginEvent(person, loginEventType);
        var orgFeideDomain = extractFeideDomainFromInputEvent(event);
        var currentCustomer = customerService.getCustomerByOrgDomain(orgFeideDomain);
        var expectedTopLevelOrgUri = currentCustomer.getCristinId();

        handler.handleRequest(event, context);

        var actualTopOrgCristinId = extractClaimFromCognitoUpdateRequest(TOP_ORG_CRISTIN_ID);
        assertThat(actualTopOrgCristinId, is(equalTo(expectedTopLevelOrgUri.toString())));
    }

    @ParameterizedTest(name = "should store user's username in cognito user attributes when user has "
                              + "many active affiliations but logged in with Feide")
    @EnumSource(value = LoginEventType.class, names = {"FEIDE"}, mode = Mode.INCLUDE)
    void shouldStoreUsersUsernameWhenUserHasManyActiveAffiliationsAndLoggedInWithFeide(
        LoginEventType loginEventType) throws NotFoundException {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions();
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
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var currentCustomer = customerService.getCustomers().stream().collect(SingletonCollector.collect());
        var expectedUsername = constructExpectedUsername(person, currentCustomer);
        var actualUsername = extractClaimFromCognitoUpdateRequest(NVA_USERNAME_CLAIM);

        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @ParameterizedTest(name =
                           "should add role (\"Creator\") that distinguishes between logged in people with active "
                           + "employments and logged in people without active employments to new user entries")
    @EnumSource(LoginEventType.class)
    void shouldAddRoleCreatorToNewUserEntries(LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var users = scanAllUsers();
        for (var user : users) {
            var roleNames = user.getRoles().stream().map(RoleDto::getRoleName).collect(Collectors.toSet());
            assertThat(roleNames, hasItem(ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT));
        }
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldAddUserAffiliationToNewUserEntryWhenUserEntryDoesNotPreexist(LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var employment = scenarios.getCristinUriForUnitAffiliations(person)
                             .stream()
                             .collect(SingletonCollector.collect());

        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var user = scanAllUsers().stream().collect(SingletonCollector.collect());
        assertThat(user.getAffiliation(), is(equalTo(employment)));
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldAddUserEmploymentMentionedInPersonRegistryToExistingUserDatabaseEntryWhenUserEntryPreexists(
        LoginEventType loginEventType)
        throws NotFoundException {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var existingUser = scenarios.createUsersForAllActiveAffiliations(person, identityService)
                               .stream().collect(SingletonCollector.collect());
        removeEmployment(existingUser);
        var userBeforeLogIn = identityService.getUser(existingUser);
        assertThat(userBeforeLogIn.getAffiliation(), is(nullValue()));

        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var userAfterLogin = identityService.getUser(existingUser);
        var expectedAffiliation = scenarios.getCristinUriForUnitAffiliations(person)
                                      .stream()
                                      .collect(SingletonCollector.collect());

        assertThat(userAfterLogin.getAffiliation(), is(equalTo(expectedAffiliation)));
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldUpdateCognitoUserInfoDetailsWithCurrentUserAffiliation(LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmployment();
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var expectedAffiliation = scenarios.getCristinUriForUnitAffiliations(person)
                                      .stream()
                                      .collect(SingletonCollector.collect());
        var cognitoAttribute = extractClaimFromCognitoUpdateRequest(PERSON_AFFILIATION_CLAIM);
        assertThat(cognitoAttribute, is(equalTo(expectedAffiliation.toString())));
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldAllowPeopleWhoAreNotRegisteredInPersonRegistryToLoginButNotGiveThemAnyRole(
        LoginEventType loginEventType) {
        var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
        var event = newLoginEvent(person, loginEventType);
        var response = handler.handleRequest(event, context);
        var accessRights = extractAccessRights(response);
        assertThat(accessRights, is((empty())));
    }

    private static String extractFeideDomainFromInputEvent(CognitoUserPoolPreTokenGenerationEvent event) {
        return event.getRequest().getUserAttributes().get(ORG_FEIDE_DOMAIN);
    }

    private static Set<AccessRight> randomAccessRights() {
        return new HashSet<>(List.of(randomElement(AccessRight.values()), randomElement(AccessRight.values())));
    }

    private static void assertThatResponseContainsAssignedAccessRights(
        UserDto existingUser,
        Set<AccessRight> assignedAccessRights,
        CognitoUserPoolPreTokenGenerationEvent response) {

        var groups =
            response.getResponse().getClaimsOverrideDetails().getGroupOverrideDetails().getGroupsToOverride();
        var groupsList = Arrays.asList(groups);
        var expectedAccessRight = constructExpectedAccessRights(existingUser, assignedAccessRights);
        assertThat(groupsList, hasItems(expectedAccessRight.toArray(String[]::new)));
    }

    private static Map<String, String> setupUserAttributesForFeideLogin(String nin,
                                                                        String feideDomain) {
        var attributes = new ConcurrentHashMap<String, String>();
        attributes.put(NIN_FOR_FEIDE_USERS, nin);
        if (nonNull(feideDomain)) {
            attributes.put(ORG_FEIDE_DOMAIN, feideDomain);
        }
        return attributes;
    }

    private void assertThatAccessRightsArePersistedInCognitoEntry(UserDto existingUserInitiallyWithoutRoles,
                                                                  Set<AccessRight> assignedAccessRights) {
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
                   .map(accessRight -> accessRight + AT + existingUserInitiallyWithoutRoles.getInstitution()
                                                              .toString())
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

    private static CognitoUserPoolPreTokenGenerationEvent nonFeideLogin(String nin) {
        var request = Request.builder()
                          .withUserAttributes(Map.of(NIN_FOR_NON_FEIDE_USERS, nin)).build();
        var loginEvent = new CognitoUserPoolPreTokenGenerationEvent();
        loginEvent.setRequest(request);
        return loginEvent;
    }

    private DynamoDBCustomerService initializeCustomerService() {
        customerServiceDatabase = new LocalCustomerServiceDatabase();
        customerServiceDatabase.setupDatabase();
        return new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());
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

    private CognitoUserPoolPreTokenGenerationEvent feideLogin(String nin) {
        var feideDomain = fetchFeideDomainFromRandomCustomerWithActiveEmployment(nin);
        var request = Request.builder().withUserAttributes(setupUserAttributesForFeideLogin(nin, feideDomain)).build();
        var loginEvent = new CognitoUserPoolPreTokenGenerationEvent();
        loginEvent.setRequest(request);
        return loginEvent;
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

    private String fetchFeideDomainFromRandomCustomerWithActiveEmployment(String nin) {
        return scenarios.fetchCustomersForPerson(nin)
                   .stream()
                   .findAny()
                   .map(CustomerDto::getFeideOrganizationDomain)
                   .orElse(null);
    }

    private CognitoUserPoolPreTokenGenerationEvent newLoginEvent(String personNin,
                                                                 LoginEventType loginEventType) {
        return LoginEventType.FEIDE.equals(loginEventType) ? feideLogin(personNin) : nonFeideLogin(personNin);
    }

    private List<UserDto> createUsersWithRolesForPerson(String nin) {
        return scenarios.createUsersForAllActiveAffiliations(nin, identityService)
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

    private List<URI> extractAllowedCustomersFromCongitoUpdateRequest() {
        return cognitoClient.getAdminUpdateUserRequest()
                   .userAttributes().stream()
                   .filter(attribute -> attribute.name().equals(ALLOWED_CUSTOMERS_CLAIM))
                   .map(AttributeType::value)
                   .map(this::extractAllowedCustomersRespectingEmptyStringNull)
                   .flatMap(Arrays::stream)
                   .collect(Collectors.toList());
    }

    private URI[] extractAllowedCustomersRespectingEmptyStringNull(String value) {
        if (EMPTY_CLAIM.equals(value)) {
            return new URI[]{};
        } else {
            return Arrays.stream(value.split(","))
                .map(URI::create)
                .collect(Collectors.toList()).toArray(URI[]::new);
        }
    }

    private String extractClaimFromCognitoUpdateRequest(String claimName) {
        return cognitoClient.getAdminUpdateUserRequest()
                   .userAttributes().stream()
                   .filter(a -> a.name().equals(claimName))
                   .map(AttributeType::value)
                   .collect(SingletonCollector.collect());
    }

    private List<URI> fetchCustomersWithActiveEmploymentsForPerson(String nin) {
        return scenarios.getCristinUriForInstitutionAffiliations(nin, ACTIVE)
                   .stream()
                   .map(attempt(cristinId -> customerService.getCustomerByCristinId(cristinId)))
                   .map(Try::orElseThrow)
                   .map(CustomerDto::getId)
                   .collect(Collectors.toList());
    }

    private String constructExpectedUsername(String nin, CustomerDto currentCustomer) {
        String personIdentifier = scenarios.getPersonFromRegistry(nin).getId();
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