package no.unit.nva.cognito;

import static java.util.Objects.nonNull;
import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.RandomUserDataGenerator.randomRoleNameButNot;
import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ELEMENTS_DELIMITER;
import static no.unit.nva.cognito.CognitoClaims.EMPTY_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.FIRST_NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.IMPERSONATED_BY_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.IMPERSONATING_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.LAST_NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ROLES_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.cognito.LoginEventType.FEIDE;
import static no.unit.nva.cognito.LoginEventType.NON_FEIDE;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.NIN_FOR_NON_FEIDE_USERS;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.ORG_FEIDE_DOMAIN;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.USER_NOT_ALLOWED_TO_IMPERSONATE;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_NAME;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_VALUE;
import static no.unit.nva.useraccessservice.model.UserDto.AT;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
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
import java.util.NoSuchElementException;
import java.util.Objects;
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
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.userceation.testing.cristin.AuthenticationScenarios;
import no.unit.nva.useraccessservice.userceation.testing.cristin.MockPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistryException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
    private final FakeSecretsManagerClient secretsManagerClient = new FakeSecretsManagerClient();
    private UserSelectionUponLoginHandler handler;
    private DynamoDBCustomerService customerService;
    private IdentityService identityService;
    private FakeCognito cognitoClient;
    private MockPersonRegistry mockPersonRegistry;
    private LocalIdentityService identityServiceDb;
    private LocalCustomerServiceDatabase customerServiceDatabase;
    private AuthenticationScenarios scenarios;

    @BeforeEach
    public void init(WireMockRuntimeInfo wireMockRuntimeInfo) throws InvalidInputException, ConflictException {
        identityService = initializeIdentityService();
        customerService = initializeCustomerService();

        String cristinUsername = randomString();
        String cristinPassword = randomString();
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_USERNAME_SECRET_KEY, cristinUsername);
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_PASSWORD_SECRET_KEY, cristinPassword);

        URI wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());

        var defaultRequestHeaders = new HttpHeaders()
                                        .withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);

        mockPersonRegistry = new MockPersonRegistry(cristinUsername,
                                                    cristinPassword,
                                                    wiremockUri,
                                                    defaultRequestHeaders);

        scenarios = new AuthenticationScenarios(mockPersonRegistry, customerService, identityService);
        cognitoClient = new FakeCognito(randomString());

        var httpClient = WiremockHttpClient.create();
        handler = new UserSelectionUponLoginHandler(cognitoClient, customerService, identityService,
                                                    CristinPersonRegistry.customPersonRegistry(
                                                        httpClient,
                                                        wiremockUri,
                                                        ServiceConstants.API_DOMAIN,
                                                        defaultRequestHeaders,
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
        var personLoggingIn = scenarios.personWithExactlyOneActiveEmployment().nin();
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

    @ParameterizedTest(name = "Login event type: {0}")
    @DisplayName("should log and throw exception when Cristin is unavailable during login")
    @EnumSource(LoginEventType.class)
    void shouldLogAndThrowExceptionWhenCristinIsUnavailableDuringLogin(LoginEventType loginEventType,
                                                                       WireMockRuntimeInfo wireMockRuntimeInfo) {

        var personLoggingIn = scenarios.personWithExactlyOneActiveEmployment().nin();
        var event = newLoginEvent(personLoggingIn, loginEventType);

        var httpClient = WiremockHttpClient.create();
        var uriWhereCristinIsUnavailable
            = URI.create("https://localhost:" + (wireMockRuntimeInfo.getHttpsPort() - 1));

        var defaultRequestHeaders = new HttpHeaders()
                                        .withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);

        var personRegistry = CristinPersonRegistry.customPersonRegistry(
            httpClient,
            uriWhereCristinIsUnavailable,
            ServiceConstants.API_DOMAIN,
            defaultRequestHeaders,
            new SecretsReader(secretsManagerClient));
        handler = new UserSelectionUponLoginHandler(cognitoClient, customerService, identityService, personRegistry);
        var testAppender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(PersonRegistryException.class, () -> handler.handleRequest(event, context));
        assertThat(testAppender.getMessages(), containsString("Cristin is unavailable"));
    }

    @ParameterizedTest(name = "Login event type: {0}")
    @DisplayName("should log and throw exception when Cristin returns bad json")
    @EnumSource(LoginEventType.class)
    void shouldLogAndThrowExceptionWhenCristinReturnsBadJsonDuringLogin(LoginEventType loginEventType) {

        var personLoggingIn = scenarios.failingPersonRegistryRequestBadJson().nin();
        var event = newLoginEvent(personLoggingIn, loginEventType);

        var testAppender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(PersonRegistryException.class, () -> handler.handleRequest(event, context));
        assertThat(testAppender.getMessages(), containsString("Got unexpected response body from Cristin"));
    }

    // TODO: is it possible to not have an active employment and be able to login through FEIDE and what should
    // happen then?
    @ParameterizedTest(name = "should not create user for the person's institution (top org) when person has not "
                              + "logged in before and has only inactive employment")
    @EnumSource(value = LoginEventType.class, names = {"NON_FEIDE"})
    void shouldNotCreateUserForPersonsTopOrganizationWhenPersonHasNotLoggedInBeforeAndHasOnlyInactiveEmployment(
        LoginEventType loginEventType) {

        var personLoggingIn = scenarios.personWithExactlyOneInactiveEmployment().nin();
        var event = newLoginEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);
        var actualUsers = scanAllUsers();
        assertThat(actualUsers, is(empty()));
    }

    @Test
    void shouldLogUserAttributesIfNinExtractFails() {
        var personLoggingIn = scenarios.personWithoutNin();
        var event = newLoginEvent(personLoggingIn.nin(), NON_FEIDE);
        var testAppender = LogUtils.getTestingAppenderForRootLogger();

        assertThrows(NoSuchElementException.class, () -> handler.handleRequest(event, context));

        assertThat(testAppender.getMessages(), containsString("Could not extract required data from request"));
        assertThat(testAppender.getMessages(), containsString("User request: CognitoUserPoolPreTokenGenerationEvent.Request"));
    }

    @ParameterizedTest(name = "Login event type: {0}")
    @DisplayName("should not create user for the person's institution if the institution is an inactive customer")
    @EnumSource(LoginEventType.class)
    void shouldNotCreateUsersWithNoCustomerWhenCustomerHasSetInactiveFromDateToThePast(LoginEventType loginEventType) {
        final var testAppender = LogUtils.getTestingAppenderForRootLogger();
        var personLoggingIn = scenarios.personWithExactlyInCustomerWithInactiveFromSetInThePast().nin();
        var event = newLoginEvent(personLoggingIn, loginEventType);
        handler.handleRequest(event, context);
        var allUsers = scanAllUsers();
        assertThat(allUsers, is(empty()));
        assertThat(testAppender.getMessages(), containsString("Customer is inactive"));
    }

    @ParameterizedTest(name = "should not create user for institutions (top orgs) that the user has only inactive "
                              + "employments with when person has not logged in before and has active and inactive "
                              + "affiliations in different institutions")
    @EnumSource(LoginEventType.class)
    void shouldNotCreateUserForTopOrgsWithInactiveEmployments(LoginEventType loginEventType) {

        var personLoggingIn =
            scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions().nin();
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
                                  .personWithOneActiveAndOneInactiveEmploymentInSameInstitution().nin();
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
            scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions().nin();
        var preExistingUsers = scenarios.createUsersForAllActiveAffiliations(personLoggingIn, identityService);
        var expectedUsernames = preExistingUsers.stream().map(UserDto::getUsername).toList();
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
        var personLoggingIn = scenarios.personWithExactlyOneActiveEmployment().nin();
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

        assertThatAccessRightsArePersistedInCognitoEntry(assignedAccessRights);
    }

    @ParameterizedTest(name = "should add customer id as custom:customerId claim when user logs in and has only one "
                              + "active employment")
    @EnumSource(LoginEventType.class)
    void shouldAddCustomerIdAsChosenCustomerIdWhenUserLogsInAndHasOnlyOneActiveEmployment(
        LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();
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
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
        var event = newLoginEvent(person, loginEventType);
        var customerFeideDomain = extractFeideDomainFromInputEvent(event);
        var expectedCustomerId = customerService.getCustomerByOrgDomain(customerFeideDomain).getId();
        handler.handleRequest(event, context);
        var actualCustomerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
        assertThat(actualCustomerId, is(equalTo(expectedCustomerId.toString())));
    }

    @ParameterizedTest(name = "should add firstName and lastName in claims when logging in")
    @EnumSource(value = LoginEventType.class)
    void shouldStoreGivenNameAndFamilyNameInUserTableWhenLoggingIn(LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);

        var expectedFirstName = scenarios.getPersonFromRegistry(person).getFirstname();
        var expectedLastName = scenarios.getPersonFromRegistry(person).getSurname();

        var firstName = extractClaimFromCognitoUpdateRequest(FIRST_NAME_CLAIM);
        var lastName = extractClaimFromCognitoUpdateRequest(LAST_NAME_CLAIM);

        assertThat(firstName, is(equalTo(expectedFirstName)));
        assertThat(lastName, is(equalTo(expectedLastName)));
    }

    @ParameterizedTest(name = "should clear customer selection claims when user has many affiliations and logs in with"
                              + "personal number")
    @EnumSource(value = LoginEventType.class, names = {"NON_FEIDE"}, mode = Mode.INCLUDE)
    void shouldClearCustomerSelectionClaimsIdWhenUserHasManyAffiliationsAndLogsInWithPersonalNumber(
        LoginEventType loginEventType) {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
        var event = newLoginEvent(person, loginEventType);

        handler.handleRequest(event, context);

        assertThatCustomerSelectionClaimsAreCleared();
    }

    @ParameterizedTest(name = "should not assign access rights for active employment when institution (top-level org) "
                              + "is not a registered customer in NVA")
    @EnumSource(LoginEventType.class)
    void shouldNotAssignAccessRightsForActiveAffiliationsWhenTopLevelOrgIsNotARegisteredCustomerInNva(
        LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmploymentInNonCustomer().nin();
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
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();

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

        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
        var expectedCustomerIds = scenarios.fetchCustomersForPerson(person)
                                      .stream()
                                      .map(CustomerDto::getId)
                                      .toList();
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

        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
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
        var person = scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions().nin();
        var expectedCustomerIds = fetchCustomersWithActiveEmploymentsForPerson(person);
        assertThat(expectedCustomerIds, hasSize(1));
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var actualAllowedCustomerIds = extractAllowedCustomersFromCongitoUpdateRequest();
        assertThat(actualAllowedCustomerIds, containsInAnyOrder(expectedCustomerIds.toArray(URI[]::new)));
    }

    @Test
    void shouldStoreAllUserRolesForActiveTopLevelAffiliationInCognitoUserAttributesForFeide() throws NotFoundException {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
        var usersWithRoles = createUsersWithRolesForPerson(person);

        var event = newLoginEvent(person, FEIDE);
        handler.handleRequest(event, context);

        var orgFeideDomain = extractFeideDomainFromInputEvent(event);
        var currentCustomer = customerService.getCustomerByOrgDomain(orgFeideDomain);

        var expectedRoles = usersWithRoles.stream()
                                .filter(u -> u.getInstitution().equals(currentCustomer.getId()))
                                .map(UserDto::getRoles)
                                .flatMap(Collection::stream)
                                .map(RoleDto::getRoleName)
                                .map(RoleName::getValue)
                                .collect(Collectors.toSet());

        var actualRoles = cognitoClient.getAdminUpdateUserRequest().userAttributes().stream()
                              .filter(attribute -> attribute.name().equals(ROLES_CLAIM))
                              .map(AttributeType::value)
                              .map(str -> str.split(","))
                              .flatMap(Arrays::stream)
                              .collect(Collectors.toList());
        assertThat(actualRoles, containsInAnyOrder(expectedRoles.toArray(String[]::new)));
    }

    @Test
    void shouldStoreNoRolesInCognitoUserAttributesForNonFeide() {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
        createUsersWithRolesForPerson(person);

        var event = newLoginEvent(person, NON_FEIDE);
        handler.handleRequest(event, context);
        var actualRoles = cognitoClient.getAdminUpdateUserRequest().userAttributes().stream()
                              .filter(attribute -> attribute.name().equals(ROLES_CLAIM))
                              .findFirst()
                              .map(AttributeType::value)
                              .get();
        assertThat(actualRoles, is(emptyString()));
    }

    @ParameterizedTest(name = "should store person's cristin Id in cognito user attributes")
    @EnumSource(LoginEventType.class)
    void shouldStorePersonCristinIdInCognitoUserAttributes(LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();
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
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();
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
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
        var event = newLoginEvent(person, loginEventType);
        var orgFeideDomain = extractFeideDomainFromInputEvent(event);
        var currentCustomer = customerService.getCustomerByOrgDomain(orgFeideDomain);
        var expectedTopLevelOrgUri = currentCustomer.getCristinId();

        handler.handleRequest(event, context);

        var actualTopOrgCristinId = extractClaimFromCognitoUpdateRequest(TOP_ORG_CRISTIN_ID);
        assertThat(actualTopOrgCristinId, is(equalTo(expectedTopLevelOrgUri.toString())));
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldNotStoreAccessRightsInCognitoWhenUserHasSeveralActiveAffiliationsAndNoActiveCustomer(
        LoginEventType loginEventType)
        throws NotFoundException, InvalidInputException, ConflictException {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutionsWithoutFeideDomain().nin();
        var role = persistRandomRole();
        createUsersWithRolesForPerson(person, role);
        addAccessRightToExistingRole(role, AccessRight.MANAGE_DEGREE);

        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);

        var actualAccessRights = extractClaimFromCognitoUpdateRequest(ACCESS_RIGHTS_CLAIM);
        assertThat(actualAccessRights, is(emptyString()));
    }

    @Test
    void shouldOnlyStoreAccessRightsInCognitoOfCurrentCustomerWhenUserHasSeveralActiveAffiliationsAndActiveCustomer()
        throws NotFoundException, InvalidInputException, ConflictException {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
        var role = persistRandomRole();
        createUsersWithRolesForPerson(person, role);
        addAccessRightToExistingRole(role, AccessRight.MANAGE_DOI);

        var event = newLoginEvent(person, FEIDE);

        handler.handleRequest(event, context);

        var actualAccessRights = extractClaimFromCognitoUpdateRequest(ACCESS_RIGHTS_CLAIM);
        var manageDoiAccessRights = Stream.of(actualAccessRights.split(ELEMENTS_DELIMITER)).filter(
            accessRight -> accessRight.equals(AccessRight.MANAGE_DOI.toPersistedString())).toList();
        assertThat(manageDoiAccessRights, hasSize(1));
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldStoreAccessRightsInCognitoWhenUserHasOneActiveAffiliations(LoginEventType loginEventType)
        throws NotFoundException, InvalidInputException, ConflictException {
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();

        var user = scenarios.createUsersForAllActiveAffiliations(person, identityService)
                       .stream()
                       .collect(SingletonCollector.collect());
        var accessRight = randomElement(AccessRight.values());
        var role = persistRoleToDatabase(List.of(accessRight));
        assignExistingRoleToUser(user, role);
        addAccessRightToExistingRole(role, accessRight);

        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);

        var actualAccessRights = extractClaimFromCognitoUpdateRequest(ACCESS_RIGHTS_CLAIM);
        assertThat(actualAccessRights, containsString(accessRight.toPersistedString()));
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldStoreAccessRightsToDatabaseWhenUserHasSeveralActiveAffiliations(LoginEventType loginEventType)
        throws InvalidInputException, NotFoundException, ConflictException {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
        var role = persistRandomRole();
        var users = createUsersWithRolesForPerson(person, role);
        var accessRight = randomElement(AccessRight.values());
        addAccessRightToExistingRole(role, accessRight);

        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);

        users.forEach(user -> {
            var userAfterLogin = attempt(() -> identityService.getUser(user)).orElseThrow();
            assertThat(userAfterLogin.getAccessRights(), hasItem(accessRight));
        });
    }

    @ParameterizedTest(name = "should store user's username in cognito user attributes when user has "
                              + "many active affiliations but logged in with Feide")
    @EnumSource(value = LoginEventType.class, names = {"FEIDE"}, mode = Mode.INCLUDE)
    void shouldStoreUsersUsernameWhenUserHasManyActiveAffiliationsAndLoggedInWithFeide(
        LoginEventType loginEventType) throws NotFoundException {
        var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
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
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();
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
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();
        var event = newLoginEvent(person, loginEventType);
        handler.handleRequest(event, context);
        var users = scanAllUsers();
        for (var user : users) {
            var roleNames = user.getRoles().stream().map(RoleDto::getRoleName).collect(Collectors.toSet());
            assertThat(roleNames, hasItem(RoleName.CREATOR));
        }
    }

    @ParameterizedTest
    @EnumSource(LoginEventType.class)
    void shouldAddUserAffiliationToNewUserEntryWhenUserEntryDoesNotPreexist(LoginEventType loginEventType) {
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();
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
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();
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
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();
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
        var person = scenarios.personThatIsNotRegisteredInPersonRegistry().nin();
        var event = newLoginEvent(person, loginEventType);
        var response = handler.handleRequest(event, context);

        var accessRights = extractAccessRights(response);
        assertThat(accessRights, is((empty())));
    }

    @Test
    void shouldUpdateUsersAccessRightsWhenRoleHasNewAccessRight()
        throws InvalidInputException, ConflictException, NotFoundException {
        var person = scenarios.personWithExactlyOneActiveEmployment().nin();
        var approveDoiAccessRight = AccessRight.MANAGE_DOI;
        var approvePublishAccessRight = AccessRight.MANAGE_PUBLISHING_REQUESTS;

        var user = scenarios.createUsersForAllActiveAffiliations(person, identityService)
                       .stream()
                       .collect(SingletonCollector.collect());
        var role = persistRoleToDatabase(List.of(approveDoiAccessRight));
        assignExistingRoleToUser(user, role);

        addAccessRightToExistingRole(role, approvePublishAccessRight);

        var event = newLoginEvent(person, LoginEventType.FEIDE);

        var response = handler.handleRequest(event, context);

        assertIdTokenContainsGroupAccessRightClaims(user, response, approveDoiAccessRight, approvePublishAccessRight);

        assertUserAccessRightsAreUpdated(user, approveDoiAccessRight,
                                         approvePublishAccessRight);
    }

    @Test
    void shouldSetCustomerSelectionClaimsOnFeideLoginWithEmploymentInBothFeideAndNonFeideCustomers() {

        var person = scenarios.personWithTwoActiveEmploymentsInNonFeideAndFeideCustomers().nin();
        var event = newLoginEvent(person, LoginEventType.FEIDE);

        var selectedCustomer = scenarios.fetchCustomersForPerson(person)
                                   .stream()
                                   .filter(customer -> Objects.nonNull(customer.getFeideOrganizationDomain()))
                                   .collect(SingletonCollector.collect());

        handler.handleRequest(event, context);

        var expectedCustomerId = selectedCustomer.getId();
        var expectedCristinId = selectedCustomer.getCristinId();
        var expectedUsername = constructExpectedUsername(person, selectedCustomer);

        assertThatCustomerSelectionClaimsArePresent(expectedCustomerId, expectedCristinId, expectedUsername);
    }

    @Test
    void shouldReturnTokenWithImpersonatedUsersAttributeWhenImpersonationIsSetAndUserIsAppAdmin()
        throws InvalidInputException, ConflictException {
        var adminName = randomString();
        var adminNin = scenarios.personWithExactlyOneActiveEmployment().nin();
        var otherPersonNin = scenarios.personWithExactlyOneActiveEmployment().nin();

        var newRole = RoleDto.newBuilder()
                          .withRoleName(RoleName.APPLICATION_ADMIN)
                          .withAccessRights(List.of(AccessRight.ACT_AS))
                          .build();
        persistRoleIfNotExist(newRole);
        createUserWithRolesForPerson(adminNin, newRole);

        var event = feideLoginWithImpersonation(adminName, adminNin, otherPersonNin);
        handler.handleRequest(event, context);

        var expectedFirstName = scenarios.getPersonFromRegistry(otherPersonNin).getFirstname();
        var expectedLastName = scenarios.getPersonFromRegistry(otherPersonNin).getSurname();

        var firstName = extractClaimFromCognitoUpdateRequest(FIRST_NAME_CLAIM);
        var lastName = extractClaimFromCognitoUpdateRequest(LAST_NAME_CLAIM);

        assertThat(firstName, is(equalTo(expectedFirstName)));
        assertThat(lastName, is(equalTo(expectedLastName)));
    }

    @Test
    void shouldThrowExceptionWhenAttemptingToImpersonateAndUserIsNotAppAdmin() {
        var notAdmin = scenarios.personWithExactlyOneActiveEmployment().nin();
        var otherPerson = scenarios.personWithExactlyOneActiveEmployment().nin();

        var event = feideLoginWithImpersonation(randomString(), notAdmin, otherPerson);

        var testAppender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(Exception.class, () -> handler.handleRequest(event, context));
        assertThat(testAppender.getMessages(), containsString(USER_NOT_ALLOWED_TO_IMPERSONATE));
    }

    @Test
    void shouldSetImpersonatedByClaimWhenImpersonating() throws InvalidInputException, ConflictException {
        var adminName = randomString();
        var adminNin = scenarios.personWithExactlyOneActiveEmployment().nin();
        var otherPersonNin = scenarios.personWithExactlyOneActiveEmployment().nin();

        var newRole = RoleDto.newBuilder()
                          .withRoleName(RoleName.APPLICATION_ADMIN)
                          .withAccessRights(List.of(AccessRight.ACT_AS))
                          .build();
        persistRoleIfNotExist(newRole);
        createUserWithRolesForPerson(adminNin, newRole);

        var event = feideLoginWithImpersonation(adminName, adminNin, otherPersonNin);
        handler.handleRequest(event, context);

        var impersonatedByClaim = extractClaimFromCognitoUpdateRequest(IMPERSONATED_BY_CLAIM);

        assertThat(impersonatedByClaim, is(equalTo(adminName)));
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
        var expectedAccessRight = constructExpectedAccessRightsForGroup(existingUser, assignedAccessRights);
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

    private static List<String> constructExpectedAccessRights(Set<AccessRight> assignedAccessRights) {
        return assignedAccessRights.stream()
                   .map(AccessRight::toPersistedString)
                   .collect(Collectors.toList());
    }

    private static List<String> constructExpectedAccessRightsForGroup(UserDto existingUserInitiallyWithoutRoles,
                                                                      Set<AccessRight> assignedAccessRights) {
        return assignedAccessRights.stream()
                   .map(accessRight -> accessRight.toPersistedString()
                                       + AT
                                       + existingUserInitiallyWithoutRoles.getInstitution()
                                             .toString())
                   .collect(Collectors.toList());
    }

    private static void assertThatEmployeeWithInactiveAndActiveEmploymentInSameTopLevelOrgGetsAUser(
        Set<URI> topLevelOrgsForCreatedUsers, Set<URI> activeEmployments, Set<URI> inactiveEmployments) {
        assertThat(topLevelOrgsForCreatedUsers, containsInAnyOrder(activeEmployments.toArray(URI[]::new)));
        assertThat(topLevelOrgsForCreatedUsers, containsInAnyOrder(inactiveEmployments.toArray(URI[]::new)));
    }

    private static CognitoUserPoolPreTokenGenerationEvent nonFeideLogin(String nin) {
        Request request;
        if (nonNull(nin))
        {
            request = Request.builder()
                              .withUserAttributes(Map.of(NIN_FOR_NON_FEIDE_USERS, nin)).build();
        }
        else {
            request = Request.builder()
                              .withUserAttributes(Map.of("SOME", "VALUE")).build();
        }
        var loginEvent = new CognitoUserPoolPreTokenGenerationEvent();
        loginEvent.setRequest(request);
        return loginEvent;
    }

    private void assertUserAccessRightsAreUpdated(UserDto user, AccessRight... accessRights) throws NotFoundException {
        var updatedUser = identityService.getUser(user);
        assertThat(updatedUser.getAccessRights(), containsInAnyOrder(accessRights));
    }

    private void assertIdTokenContainsGroupAccessRightClaims(UserDto user,
                                                             CognitoUserPoolPreTokenGenerationEvent response,
                                                             AccessRight... accessRights) {
        var actualAccessRightClaims = extractAccessRights(response);
        var expectedAccessRightClaims =
            constructExpectedAccessRightsForGroup(user, Set.of(accessRights)).toArray();
        assertThat(actualAccessRightClaims, containsInAnyOrder(expectedAccessRightClaims));
    }

    private void assertThatCustomerSelectionClaimsAreCleared() {
        final var customerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
        assertThat(customerId, is(equalTo(EMPTY_CLAIM)));

        final var topOrgCristinId = extractClaimFromCognitoUpdateRequest(TOP_ORG_CRISTIN_ID);
        assertThat(topOrgCristinId, is(equalTo(EMPTY_CLAIM)));

        final var nvaUsername = extractClaimFromCognitoUpdateRequest(NVA_USERNAME_CLAIM);
        assertThat(nvaUsername, is(equalTo(EMPTY_CLAIM)));

        final var personAffiliation = extractClaimFromCognitoUpdateRequest(PERSON_AFFILIATION_CLAIM);
        assertThat(personAffiliation, is(equalTo(EMPTY_CLAIM)));
    }

    private void assertThatCustomerSelectionClaimsArePresent(URI expectedCustomerId,
                                                             URI expectedCristinId,
                                                             String expectedUsername) {

        final var customerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
        assertThat(customerId, is(equalTo(expectedCustomerId.toString())));

        final var topOrgCristinId = extractClaimFromCognitoUpdateRequest(TOP_ORG_CRISTIN_ID);
        assertThat(topOrgCristinId, is(equalTo(expectedCristinId.toString())));

        final var nvaUsername = extractClaimFromCognitoUpdateRequest(NVA_USERNAME_CLAIM);
        assertThat(nvaUsername, is(equalTo(expectedUsername)));

        final var personAffiliation = extractClaimFromCognitoUpdateRequest(PERSON_AFFILIATION_CLAIM);
        assertThat(personAffiliation, is(notNullValue()));
    }

    private void assertThatAccessRightsArePersistedInCognitoEntry(Set<AccessRight> assignedAccessRights) {
        var accessRightsPersistedInCognito = extractAccessRightFromCognitoEntry();
        var expectedAccessRights = constructExpectedAccessRights(assignedAccessRights);
        for (var expectedAccessRight : expectedAccessRights) {
            assertThat(accessRightsPersistedInCognito, containsString(expectedAccessRight));
        }
    }

    private void assignExistingRoleToUser(UserDto existingUser, RoleDto role) throws NotFoundException {
        existingUser.setRoles(Set.of(role));
        identityService.updateUser(existingUser);
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
        var roleDto = RoleDto.newBuilder().withRoleName(randomRoleNameButNot(RoleName.CREATOR)).withAccessRights(accessRights).build();
        persistRoleIfNotExist(roleDto);
        return identityService.getRole(roleDto);
    }

    private void addAccessRightToExistingRole(RoleDto role, AccessRight accessRight)
        throws InvalidInputException, NotFoundException {
        var accessRights = new HashSet<>(role.getAccessRights());
        accessRights.add(accessRight);
        var updatedRole = RoleDto.newBuilder()
                              .withRoleName(role.getRoleName())
                              .withAccessRights(accessRights)
                              .build();
        updateRole(updatedRole);
    }

    private String fetchFeideDomainFromRandomCustomerWithActiveEmployment(String nin) {
        return scenarios.fetchCustomersForPerson(nin)
                   .stream()
                   .filter(customer -> Objects.nonNull(customer.getFeideOrganizationDomain()))
                   .findAny()
                   .map(CustomerDto::getFeideOrganizationDomain)
                   .orElse(null);
    }

    private CognitoUserPoolPreTokenGenerationEvent newLoginEvent(String personNin,
                                                                 LoginEventType loginEventType) {
        return LoginEventType.FEIDE.equals(loginEventType) ? feideLogin(personNin) : nonFeideLogin(personNin);
    }

    private CognitoUserPoolPreTokenGenerationEvent feideLoginWithImpersonation(String adminUsername,
                                                                               String adminNin,
                                                                               String impersonatedNin) {

        var attributes = new ConcurrentHashMap<String, String>();
        attributes.put(NIN_FOR_FEIDE_USERS, adminNin);
        attributes.put(IMPERSONATING_CLAIM, impersonatedNin);

        var request = Request.builder().withUserAttributes(attributes).build();
        var loginEvent = new CognitoUserPoolPreTokenGenerationEvent();
        loginEvent.setRequest(request);
        loginEvent.setUserName(adminUsername);
        return loginEvent;
    }

    private List<UserDto> createUsersWithRolesForPerson(String nin) {
        return scenarios.createUsersForAllActiveAffiliations(nin, identityService)
                   .stream()
                   .map(attempt(user -> addRoleToUser(user, persistRandomRole())))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private List<UserDto> createUsersWithRolesForPerson(String nin, RoleDto role) {
        return scenarios.createUsersForAllActiveAffiliations(nin, identityService)
                   .stream()
                   .map(attempt(user -> addRoleToUser(user, role)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private List<UserDto> createUserWithRolesForPerson(String nin, RoleDto role) {
        return scenarios.createUsersForAllActiveAffiliations(nin, identityService)
                   .stream()
                   .map(attempt(user -> addRoleToUser(user, role)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private UserDto addRoleToUser(UserDto user, RoleDto persistRandomRole) throws NotFoundException {
        user.setRoles(Set.of(persistRandomRole));
        identityService.updateUser(user);
        return identityService.getUser(user);
    }

    private RoleDto persistRandomRole() throws InvalidInputException, ConflictException {
        var newRole = RoleDto.newBuilder()
                          .withRoleName(randomRoleName())
                          .withAccessRights(randomAccessRights())
                          .build();
        persistRoleIfNotExist(newRole.copy().withRoleName(newRole.getRoleName()).build());
        return attempt(() -> identityService.getRole(newRole)).orElseThrow();
    }

    private void persistRoleIfNotExist(RoleDto newRole) throws ConflictException, InvalidInputException {
        if (attempt(() -> identityService.getRole(newRole)).toOptional().isEmpty()) {
            identityService.addRole(newRole);
        }
    }

    private void updateRole(RoleDto updatedRole) throws InvalidInputException, NotFoundException {
        identityService.updateRole(updatedRole);
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
                       .toList()
                       .toArray(URI[]::new);
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