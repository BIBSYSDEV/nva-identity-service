package no.unit.nva.cognito;

import static java.util.Objects.nonNull;
import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.RandomUserDataGenerator.randomRoleNameButNot;
import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_TERMS;
import static no.unit.nva.cognito.CognitoClaims.CUSTOMER_ACCEPTED_TERMS;
import static no.unit.nva.cognito.CognitoClaims.ELEMENTS_DELIMITER;
import static no.unit.nva.cognito.CognitoClaims.EMPTY_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.FEIDE_ID;
import static no.unit.nva.cognito.CognitoClaims.FIRST_NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.IDENTITIES_ATTRIBUTE;
import static no.unit.nva.cognito.CognitoClaims.LAST_NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.cognito.CognitoClaims.NIN_FOR_NON_FEIDE_USERS;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ROLES_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.cognito.CognitoClaims.VIEWING_SCOPE_EXCLUDED_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.VIEWING_SCOPE_INCLUDED_CLAIM;
import static no.unit.nva.cognito.LoginEventType.FEIDE;
import static no.unit.nva.cognito.LoginEventType.NON_FEIDE;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.ORG_FEIDE_DOMAIN;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.TRIGGER_SOURCE_CLIENT_CREDENTIALS;
import static no.unit.nva.database.TermsAndConditionsService.TERMS_TABLE_NAME_ENV;
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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.FakeCognito;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.DatabaseTestConfig;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.database.SingleTableTemplateCreator;
import no.unit.nva.database.TermsAndConditionsService;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.userceation.testing.cristin.AuthenticationScenarios;
import no.unit.nva.useraccessservice.userceation.testing.cristin.MockPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.ActiveCustomers;
import no.unit.nva.useraccessservice.usercreation.UserCreationContext;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceMissingLinkException;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogRecorder;
import nva.commons.secrets.SecretsReader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * In production the inbound federation trigger runs before this handler at every federated login.
 * It looks the person up in the registry, creates the user entries and writes the link and person
 * rows. {@link HandlerBehindInboundTrigger} does the same here, so the tests can start from a
 * person in the registry. The tests keep the identity number on the login event so that the
 * stand-in knows who is logging in; the handler never reads it.
 */
@WireMockTest(httpsEnabled = true)
class UserSelectionUponLoginHandlerTest {

  public static final int SINGLE_EXPECTED_USER = 1;
  public static final boolean ACTIVE = true;
  public static final String TERMS_TABLE = "TermsTable";
  public static final URI TERMS_URI = URI.create("https://nva.sikt.no/terms/2024-10-01");
  public static final String TRIGGER_SOURCE_HOSTED_AUTH = "TokenGeneration_HostedAuth";
  public static final String TRIGGER_SOURCE_AUTHENTICATION = "TokenGeneration_Authentication";
  public static final String TRIGGER_SOURCE_REFRESH_TOKENS = "TokenGeneration_RefreshTokens";
  private static final String COMMA = ",";
  private static final String EXAMPLE_NAME = "Håkon Østby Ærland";
  private static final String FEDERATED_USERNAME_PREFIX = "Dataporten_";
  private static final String FEDERATED_IDENTITIES =
      "[{\"providerName\":\"Dataporten\",\"providerType\":\"OIDC\",\"primary\":\"true\"}]";
  private static final Set<String> NIN_ATTRIBUTES =
      Set.of(NIN_FOR_NON_FEIDE_USERS, NIN_FOR_FEIDE_USERS);

  private final Context context = new FakeContext();
  private final FakeSecretsManagerClient secretsManagerClient = new FakeSecretsManagerClient();
  private UserSelectionUponLoginHandler handler;
  private DynamoDBCustomerService customerService;
  private IdentityService identityService;
  private TermsAndConditionsService termsAndConditionsService;
  private FakeCognito cognitoClient;
  private MockPersonRegistry mockPersonRegistry;
  private LocalIdentityService identityServiceDb;
  private LocalCustomerServiceDatabase customerServiceDatabase;
  private AuthenticationScenarios scenarios;
  private PersonRegistry personRegistry;

  @BeforeEach
  public void init(WireMockRuntimeInfo wireMockRuntimeInfo)
      throws InvalidInputException, ConflictException {
    identityService = initializeIdentityService();
    customerService = initializeCustomerService();

    String cristinUsername = randomString();
    String cristinPassword = randomString();
    secretsManagerClient.putSecret(
        CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_USERNAME_SECRET_KEY, cristinUsername);
    secretsManagerClient.putSecret(
        CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_PASSWORD_SECRET_KEY, cristinPassword);

    URI wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());

    var defaultRequestHeaders =
        new HttpHeaders().withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);

    mockPersonRegistry =
        new MockPersonRegistry(
            cristinUsername, cristinPassword, wiremockUri, defaultRequestHeaders);

    scenarios = new AuthenticationScenarios(mockPersonRegistry, customerService, identityService);
    cognitoClient = new FakeCognito();
    var environment = mock(Environment.class);
    when(environment.readEnv(TERMS_TABLE_NAME_ENV)).thenReturn(TERMS_TABLE);

    DynamoDbClient embeddedClient = DatabaseTestConfig.getEmbeddedClient();
    new SingleTableTemplateCreator(embeddedClient).createTable(TERMS_TABLE);

    termsAndConditionsService = new TermsAndConditionsService(embeddedClient, environment);

    var httpClient = WiremockHttpClient.create();
    this.personRegistry =
        spy(
            CristinPersonRegistry.customPersonRegistry(
                httpClient,
                wiremockUri,
                ServiceConstants.API_DOMAIN,
                defaultRequestHeaders,
                new SecretsReader(secretsManagerClient)));

    handler = new HandlerBehindInboundTrigger();
  }

  /** The handler alone, for tests that write the link and person rows themselves. */
  private UserSelectionUponLoginHandler handlerAlone() {
    return new UserSelectionUponLoginHandler(
        cognitoClient, customerService, identityService, termsAndConditionsService);
  }

  private final class HandlerBehindInboundTrigger extends UserSelectionUponLoginHandler {

    private HandlerBehindInboundTrigger() {
      super(cognitoClient, customerService, identityService, termsAndConditionsService);
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEventV2 handleRequest(
        CognitoUserPoolPreTokenGenerationEventV2 input, Context context) {
      if (TRIGGER_SOURCE_HOSTED_AUTH.equals(input.getTriggerSource())) {
        var attributes = input.getRequest().getUserAttributes();
        Optional.ofNullable(attributes.get(NIN_FOR_FEIDE_USERS))
            .or(() -> Optional.ofNullable(attributes.get(NIN_FOR_NON_FEIDE_USERS)))
            .flatMap(NationalIdentityNumber::fromDigits)
            .flatMap(personRegistry::fetchPersonByNin)
            .ifPresent(person -> writeRowsAsTheInboundTriggerDoes(input, person));
      }
      return super.handleRequest(input, context);
    }

    private void writeRowsAsTheInboundTriggerDoes(
        CognitoUserPoolPreTokenGenerationEventV2 input, Person person) {
      var attributes = input.getRequest().getUserAttributes();
      var feideIdentifier = attributes.get(FEIDE_ID);
      var customers = ActiveCustomers.forPerson(person, customerService);
      var creator = new UserEntriesCreatorForPerson(identityService);
      creator.createUsers(new UserCreationContext(person, customers, feideIdentifier));
      creator.writeLinkAndPersonRows(
          input.getUserName(),
          feideIdentifier,
          attributes.get(ORG_FEIDE_DOMAIN),
          person,
          customers,
          Instant.now());
    }
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

  @AfterEach
  public void tearDown() {
    identityServiceDb.closeDB();
    customerServiceDatabase.deleteDatabase();
  }

  private List<UserDto> scanAllUsers() {
    var request = new ScanDatabaseRequestV2(randomString(), 100, null);
    return identityService.fetchOnePageOfUsers(request).getRetrievedUsers();
  }

  private CognitoUserPoolPreTokenGenerationEventV2 newLoginEvent(
      String personNin, LoginEventType loginEventType) {
    return LoginEventType.FEIDE.equals(loginEventType)
        ? feideLogin(personNin)
        : nonFeideLogin(personNin);
  }

  private static CognitoUserPoolPreTokenGenerationEventV2 nonFeideLogin(String nin, String name) {
    Request request;
    if (nonNull(nin)) {
      Map<String, String> userAttributes = new HashMap<>();
      userAttributes.put(NIN_FOR_NON_FEIDE_USERS, nin);
      if (name != null) {
        userAttributes.put(NAME_CLAIM, name);
      }
      userAttributes.put(CUSTOMER_ACCEPTED_TERMS, TERMS_URI.toString());
      userAttributes.put(CURRENT_TERMS, TERMS_URI.toString());
      userAttributes.put(IDENTITIES_ATTRIBUTE, FEDERATED_IDENTITIES);
      request = Request.builder().withUserAttributes(userAttributes).build();
    } else {
      request =
          Request.builder()
              .withUserAttributes(
                  Map.of("SOME", "VALUE", IDENTITIES_ATTRIBUTE, FEDERATED_IDENTITIES))
              .build();
    }
    var loginEvent = new CognitoUserPoolPreTokenGenerationEventV2();
    loginEvent.setTriggerSource(TRIGGER_SOURCE_HOSTED_AUTH);
    loginEvent.setRequest(request);
    loginEvent.setUserName(randomString());
    return loginEvent;
  }

  private static CognitoUserPoolPreTokenGenerationEventV2 nonFeideLogin(String nin) {
    return nonFeideLogin(nin, EXAMPLE_NAME);
  }

  private CognitoUserPoolPreTokenGenerationEventV2 feideLogin(String nin) {
    var feideDomain = fetchFeideDomainFromRandomCustomerWithActiveEmployment(nin);
    var request =
        Request.builder()
            .withUserAttributes(setupUserAttributesForFeideLogin(nin, feideDomain))
            .build();
    var loginEvent = new CognitoUserPoolPreTokenGenerationEventV2();
    loginEvent.setTriggerSource(TRIGGER_SOURCE_HOSTED_AUTH);
    loginEvent.setRequest(request);
    loginEvent.setUserName(FEDERATED_USERNAME_PREFIX + randomString());
    return loginEvent;
  }

  private static Map<String, String> setupUserAttributesForFeideLogin(
      String nin, String feideDomain) {
    var attributes = new ConcurrentHashMap<String, String>();
    attributes.put(NIN_FOR_FEIDE_USERS, nin);
    attributes.put(IDENTITIES_ATTRIBUTE, FEDERATED_IDENTITIES);
    attributes.put(FEIDE_ID, "feideid@domain.no");
    attributes.put(FIRST_NAME_CLAIM, "[%22L%C3%B8k%22]");
    attributes.put(LAST_NAME_CLAIM, "[%22R%C3%A6l%20%C3%85l%22]");
    if (nonNull(feideDomain)) {
      attributes.put(ORG_FEIDE_DOMAIN, feideDomain);
    }
    return attributes;
  }

  private String fetchFeideDomainFromRandomCustomerWithActiveEmployment(String nin) {
    return scenarios.fetchCustomersForPerson(nin).stream()
        .filter(customer -> Objects.nonNull(customer.getFeideOrganizationDomain()))
        .findAny()
        .map(CustomerDto::getFeideOrganizationDomain)
        .orElse(null);
  }

  @ParameterizedTest(
      name =
          "should return access rights as user groups for user within the scope of a customer "
              + "for user's active top orgs")
  @EnumSource(LoginEventType.class)
  void shouldReturnAccessRightsForUserConcatenatedWithCustomerNvaIdentifierForUsersActiveTopOrgs(
      LoginEventType eventType) throws ConflictException, NotFoundException, InvalidInputException {
    var personLoggingIn = scenarios.personWithExactlyOneActiveEmployment().nin();
    var existingUserInitiallyWithoutRoles =
        scenarios.createUsersForAllActiveAffiliations(personLoggingIn, identityService).stream()
            .collect(SingletonCollector.collect());
    termsAndConditionsService.updateTermsAndConditions(
        existingUserInitiallyWithoutRoles.getCristinId(),
        TERMS_URI,
        existingUserInitiallyWithoutRoles.getUsername());
    var assignedAccessRights = randomAccessRights();
    var role = persistRoleToDatabase(assignedAccessRights);
    assignExistingRoleToUser(existingUserInitiallyWithoutRoles, role);
    var event = newLoginEvent(personLoggingIn, eventType);
    var response = handler.handleRequest(event, context);
    assertThatResponseContainsAssignedAccessRights(
        existingUserInitiallyWithoutRoles, assignedAccessRights, response);

    assertThatAccessRightsArePersistedInCognitoEntry(assignedAccessRights);
  }

  @Test
  void shouldNotReturnAccessRightsWhenLatestTermsIsNotAccepted()
      throws InvalidInputException, ConflictException, NotFoundException {
    var personLoggingIn = scenarios.personWithExactlyOneActiveEmployment().nin();
    var user =
        scenarios.createUsersForAllActiveAffiliations(personLoggingIn, identityService).stream()
            .collect(SingletonCollector.collect());
    var assignedAccessRights = randomAccessRights();
    var role = persistRoleToDatabase(assignedAccessRights);
    assignExistingRoleToUser(user, role);
    var event = newLoginEvent(personLoggingIn, FEIDE);
    var response = handler.handleRequest(event, context);
    String[] groupsToOverride =
        response
            .getResponse()
            .getClaimsAndScopeOverrideDetails()
            .getGroupOverrideDetails()
            .getGroupsToOverride();
    assertThat(groupsToOverride.length, is(equalTo(0)));
  }

  private static Set<AccessRight> randomAccessRights() {
    return new HashSet<>(
        List.of(randomElement(AccessRight.values()), randomElement(AccessRight.values())));
  }

  private static void assertThatResponseContainsAssignedAccessRights(
      UserDto existingUser,
      Set<AccessRight> assignedAccessRights,
      CognitoUserPoolPreTokenGenerationEventV2 response) {

    var groups =
        response
            .getResponse()
            .getClaimsAndScopeOverrideDetails()
            .getGroupOverrideDetails()
            .getGroupsToOverride();
    var groupsList = Arrays.asList(groups);
    var expectedAccessRight =
        constructExpectedAccessRightsForGroup(existingUser, assignedAccessRights);
    assertThat(groupsList, hasItems(expectedAccessRight.toArray(String[]::new)));
  }

  private static List<String> constructExpectedAccessRightsForGroup(
      UserDto existingUserInitiallyWithoutRoles, Set<AccessRight> assignedAccessRights) {
    return assignedAccessRights.stream()
        .map(accessRight ->
            accessRight.toPersistedString()
                + AT
                + existingUserInitiallyWithoutRoles.getInstitution().toString())
        .collect(Collectors.toList());
  }

  private void assertThatAccessRightsArePersistedInCognitoEntry(
      Set<AccessRight> assignedAccessRights) {
    var accessRightsPersistedInCognito = extractAccessRightFromCognitoEntry();
    var expectedAccessRights = constructExpectedAccessRights(assignedAccessRights);
    for (var expectedAccessRight : expectedAccessRights) {
      assertThat(accessRightsPersistedInCognito, containsString(expectedAccessRight));
    }
  }

  private static List<String> constructExpectedAccessRights(Set<AccessRight> assignedAccessRights) {
    return assignedAccessRights.stream()
        .map(AccessRight::toPersistedString)
        .collect(Collectors.toList());
  }

  private String extractAccessRightFromCognitoEntry() {
    return fetchCognitoEntryUpdateRequestSentByHandler().userAttributes().stream()
        .filter(userAttribute -> ACCESS_RIGHTS_CLAIM.equals(userAttribute.name()))
        .map(AttributeType::value)
        .collect(SingletonCollector.collect());
  }

  private AdminUpdateUserAttributesRequest fetchCognitoEntryUpdateRequestSentByHandler() {
    return cognitoClient.getAdminUpdateUserRequest();
  }

  private void assignExistingRoleToUser(UserDto existingUser, RoleDto role)
      throws NotFoundException {
    existingUser.setRoles(Set.of(role));
    identityService.updateUser(existingUser);
  }

  private RoleDto persistRoleToDatabase(Collection<AccessRight> accessRights)
      throws InvalidInputException, ConflictException, NotFoundException {
    var roleDto =
        RoleDto.newBuilder()
            .withRoleName(randomRoleNameButNot(RoleName.CREATOR))
            .withAccessRights(accessRights)
            .build();
    persistRoleIfNotExist(roleDto);
    return identityService.getRole(roleDto);
  }

  private void persistRoleIfNotExist(RoleDto newRole)
      throws ConflictException, InvalidInputException {
    if (attempt(() -> identityService.getRole(newRole)).toOptional().isEmpty()) {
      identityService.addRole(newRole);
    }
  }

  @ParameterizedTest(
      name =
          "should add customer id as custom:customerId claim when user logs in and has only one "
              + "active employment")
  @EnumSource(LoginEventType.class)
  void shouldAddCustomerIdAsChosenCustomerIdWhenUserLogsInAndHasOnlyOneActiveEmployment(
      LoginEventType loginEventType) {
    var person = scenarios.personWithExactlyOneActiveEmployment().nin();
    var expectedCustomerId =
        scenarios.fetchCustomersForPerson(person).stream()
            .collect(SingletonCollector.collect())
            .getId();

    var event = newLoginEvent(person, loginEventType);
    handler.handleRequest(event, context);

    var actualCustomerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
    assertThat(actualCustomerId, is(equalTo(expectedCustomerId.toString())));
  }

  private String extractClaimFromCognitoUpdateRequest(String claimName) {
    return cognitoClient.getAdminUpdateUserRequest().userAttributes().stream()
        .filter(a -> a.name().equals(claimName))
        .map(AttributeType::value)
        .collect(SingletonCollector.collect());
  }

  @ParameterizedTest(
      name =
          "should add Feide specified customer id as current customer id when user logs in "
              + "with feide")
  @EnumSource(
      value = LoginEventType.class,
      names = {"FEIDE"},
      mode = Mode.INCLUDE)
  void shouldAddFeideSpecifiedCustomerIdAsCurrentCustomerIdWhenUserLogsInWithFeide(
      LoginEventType loginEventType) throws NotFoundException {
    var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    var event = newLoginEvent(person, loginEventType);
    var customerFeideDomain = extractFeideDomainFromInputEvent(event);
    var expectedCustomerId = customerService.getCustomerByOrgDomain(customerFeideDomain).getId();
    handler.handleRequest(event, context);
    var actualCustomerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
    assertThat(actualCustomerId, is(equalTo(expectedCustomerId.toString())));
  }

  private static String extractFeideDomainFromInputEvent(
      CognitoUserPoolPreTokenGenerationEventV2 event) {
    return event.getRequest().getUserAttributes().get(ORG_FEIDE_DOMAIN);
  }

  @Test
  void shouldIssueClaimsForOwnPersonWhenStaleImpersonatingAttributeIsPresent() {
    var person = scenarios.personWithExactlyOneActiveEmployment().nin();
    var otherPerson = scenarios.personWithExactlyOneActiveEmployment().nin();
    var event = feideLogin(person);
    event.getRequest().getUserAttributes().put("custom:impersonating", otherPerson);

    handler.handleRequest(event, context);

    var actualCristinPersonId =
        URI.create(extractClaimFromCognitoUpdateRequest(PERSON_CRISTIN_ID_CLAIM));
    assertThat(actualCristinPersonId, is(equalTo(scenarios.getCristinIdForPerson(person))));
    assertThat(
        actualCristinPersonId, is(not(equalTo(scenarios.getCristinIdForPerson(otherPerson)))));
  }

  private static String extractFeideIdentifierFromInputEvent(
      CognitoUserPoolPreTokenGenerationEventV2 event) {
    return event.getRequest().getUserAttributes().get(FEIDE_ID);
  }

  @ParameterizedTest(name = "Login event type: {0}")
  @DisplayName(
      "should give the same claims from the link on refresh as from the person registry, without"
          + " calling the registry")
  @EnumSource(LoginEventType.class)
  void shouldGiveSameClaimsFromLinkOnRefreshAsFromPersonRegistry(LoginEventType loginEventType)
      throws NotFoundException, InvalidInputException, ConflictException {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var role = persistRoleToDatabase(randomAccessRights());
    createUsersWithRolesForPerson(nin, role);
    termsAndConditionsService.updateTermsAndConditions(
        scenarios.getCristinIdForPerson(nin), TERMS_URI, randomString());
    var login = newLoginEvent(nin, loginEventType);
    var loginResponse = handler.handleRequest(login, context);
    var claimsFromRegistry = cognitoClient.getAdminUpdateUserRequest().userAttributes();
    var groupsFromRegistry = groupsToOverride(loginResponse);
    var selectedCustomer = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);

    var refresh = refreshFor(login);
    refresh.getRequest().getUserAttributes().put(CURRENT_CUSTOMER_CLAIM, selectedCustomer);
    var refreshResponse = handler.handleRequest(refresh, context);

    assertThat(groupsFromRegistry, is(not(empty())));
    assertThat(extractClaimFromCognitoUpdateRequest(ROLES_CLAIM), is(not(emptyString())));
    assertThat(extractClaimFromCognitoUpdateRequest(ACCESS_RIGHTS_CLAIM), is(not(emptyString())));
    assertThat(
        cognitoClient.getAdminUpdateUserRequest().userAttributes(),
        is(equalTo(claimsFromRegistry)));
    assertThat(groupsToOverride(refreshResponse), is(equalTo(groupsFromRegistry)));
  }

  private static List<String> groupsToOverride(CognitoUserPoolPreTokenGenerationEventV2 event) {
    return Arrays.asList(
        event
            .getResponse()
            .getClaimsAndScopeOverrideDetails()
            .getGroupOverrideDetails()
            .getGroupsToOverride());
  }

  @Test
  void shouldTakeFeideDetailsFromLinkOnRefreshWithoutNinAndFeideAttributes() {
    var nin = scenarios.personWithTwoActiveEmploymentsInNonFeideAndFeideCustomers().nin();
    var login = feideLogin(nin);
    handler.handleRequest(login, context);
    var customerFromRegistry = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
    var feideIdFromRegistry = extractClaimFromCognitoUpdateRequest(FEIDE_ID);

    var withoutFeideAttributes = refreshFor(withoutNin(login));
    withoutFeideAttributes
        .getRequest()
        .getUserAttributes()
        .put(CURRENT_CUSTOMER_CLAIM, customerFromRegistry);
    withoutFeideAttributes.getRequest().getUserAttributes().remove(FEIDE_ID);
    withoutFeideAttributes.getRequest().getUserAttributes().remove(ORG_FEIDE_DOMAIN);
    handler.handleRequest(withoutFeideAttributes, context);

    assertThat(customerFromRegistry, is(not(equalTo(EMPTY_CLAIM))));
    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM),
        is(equalTo(customerFromRegistry)));
    assertThat(extractClaimFromCognitoUpdateRequest(FEIDE_ID), is(equalTo(feideIdFromRegistry)));
  }

  @Test
  void shouldGiveNoAccessForUserEntryAtCustomerThatNoLongerExistsWhenPasswordUserLogsIn()
      throws NotFoundException, InvalidInputException, ConflictException {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var cristinId = scenarios.getCristinIdForPerson(nin);
    var role = persistRoleToDatabase(randomAccessRights());
    var user =
        scenarios.createUsersForAllActiveAffiliations(nin, identityService).stream()
            .map(attempt(created -> addRoleToUser(created, role)))
            .map(Try::orElseThrow)
            .collect(SingletonCollector.collect());
    identityService.addUser(
        user.copy()
            .withUsername(randomString())
            .withInstitution(nonExistingCustomerId())
            .withInstitutionCristinId(randomUri())
            .build());
    termsAndConditionsService.updateTermsAndConditions(cristinId, TERMS_URI, user.getUsername());
    var login = passwordLogin(cristinId, null);

    var response = handler.handleRequest(login, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM),
        is(equalTo(user.getInstitution().toString())));
    assertThat(
        extractClaimFromCognitoUpdateRequest(ALLOWED_CUSTOMERS_CLAIM),
        is(equalTo(user.getInstitution().toString())));
    assertThat(groupsToOverride(response), is(not(empty())));
  }

  @Test
  void shouldLogTheFailureWhenUserEntryLookupThroughIndexFailsOnLinkPath() {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var login = feideLogin(nin);
    var feideIdentifier = extractFeideIdentifierFromInputEvent(login);
    scenarios.createLegacyUsersForAllActiveAffiliations(nin, feideIdentifier, identityService);
    handler.handleRequest(login, context);
    var failingIdentityService = spy(identityService);
    doThrow(new RuntimeException("Index unavailable"))
        .when(failingIdentityService)
        .getUserByPersonCristinIdAndCustomerCristinId(any(), any());
    handler =
        new UserSelectionUponLoginHandler(
            cognitoClient, customerService, failingIdentityService, termsAndConditionsService);
    var logRecorder = LogRecorder.forRoot(UserSelectionUponLoginHandlerTest.class);
    var refresh = refreshFor(withoutNin(login));
    refresh
        .getRequest()
        .getUserAttributes()
        .put(
            CURRENT_CUSTOMER_CLAIM,
            fetchCustomersWithActiveEmploymentsForPerson(nin).get(0).toString());

    handler.handleRequest(refresh, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM), is(equalTo(EMPTY_CLAIM)));
    Assertions.assertThat(logRecorder.asString())
        .contains("No user entry for person")
        .contains("Index unavailable");
  }

  @Test
  void shouldResolvePasswordUserFromAdminSetCristinIdOnLoginAndRefreshWithoutNin()
      throws NotFoundException, InvalidInputException, ConflictException {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var cristinId = scenarios.getCristinIdForPerson(nin);
    var role = persistRoleToDatabase(randomAccessRights());
    var user =
        scenarios.createUsersForAllActiveAffiliations(nin, identityService).stream()
            .map(attempt(created -> addRoleToUser(created, role)))
            .map(Try::orElseThrow)
            .collect(SingletonCollector.collect());
    termsAndConditionsService.updateTermsAndConditions(cristinId, TERMS_URI, user.getUsername());
    var login = passwordLogin(cristinId, null);

    var loginResponse = handler.handleRequest(login, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(PERSON_CRISTIN_ID_CLAIM),
        is(equalTo(cristinId.toString())));
    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM),
        is(equalTo(user.getInstitution().toString())));
    assertThat(
        extractClaimFromCognitoUpdateRequest(NVA_USERNAME_CLAIM), is(equalTo(user.getUsername())));
    assertThat(
        extractClaimFromCognitoUpdateRequest(FIRST_NAME_CLAIM), is(equalTo(user.getGivenName())));
    assertThat(
        extractClaimFromCognitoUpdateRequest(LAST_NAME_CLAIM), is(equalTo(user.getFamilyName())));
    assertThat(
        extractClaimFromCognitoUpdateRequest(ROLES_CLAIM),
        containsString(role.getRoleName().getValue()));
    assertThat(extractClaimFromCognitoUpdateRequest(ACCESS_RIGHTS_CLAIM), is(not(emptyString())));
    assertThat(groupsToOverride(loginResponse), is(not(empty())));

    var refresh = passwordLogin(cristinId, null);
    refresh.setUserName(login.getUserName());
    refresh.setTriggerSource(TRIGGER_SOURCE_REFRESH_TOKENS);
    refresh
        .getRequest()
        .getUserAttributes()
        .put(CURRENT_CUSTOMER_CLAIM, user.getInstitution().toString());
    handler.handleRequest(refresh, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM),
        is(equalTo(user.getInstitution().toString())));
  }

  @Test
  void shouldIgnoreNinForPasswordUser() {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    scenarios.createUsersForAllActiveAffiliations(nin, identityService);
    var cristinId = scenarios.getCristinIdForPerson(nin);
    var event = passwordLogin(cristinId, nin);

    handler.handleRequest(event, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(PERSON_CRISTIN_ID_CLAIM),
        is(equalTo(cristinId.toString())));
  }

  @ParameterizedTest(name = "custom:cristinId: {0}")
  @NullSource
  @ValueSource(strings = {" ", "not a uri"})
  void shouldFailWithMissingLinkWhenPasswordUserHasNoUsableCristinId(String cristinId) {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var event = passwordLogin(null, nin);
    if (nonNull(cristinId)) {
      event.getRequest().getUserAttributes().put(PERSON_CRISTIN_ID_CLAIM, cristinId);
    }

    var exception =
        assertThrows(
            IdentityServiceMissingLinkException.class, () -> handler.handleRequest(event, context));

    assertThat(exception.getErrorCode(), is(equalTo("1004")));
  }

  @Test
  void shouldNotGrantAccessToCustomerThatIsNoLongerRememberedForPerson() throws NotFoundException {
    var nin = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    var cristinId = scenarios.getCristinIdForPerson(nin);
    termsAndConditionsService.updateTermsAndConditions(cristinId, TERMS_URI, randomString());
    var login = newLoginEvent(nin, NON_FEIDE);
    handler.handleRequest(login, context);
    var remembered = identityService.fetchLinkedPerson(cristinId).orElseThrow();
    var remainingCustomer = remembered.customers().iterator().next();
    identityService.saveLinkedPerson(
        new LinkedPerson(
            cristinId,
            remembered.givenName(),
            remembered.familyName(),
            Set.of(remainingCustomer),
            remembered.lastVerified()));

    var refresh = refreshFor(withoutNin(login));
    refresh
        .getRequest()
        .getUserAttributes()
        .put(CURRENT_CUSTOMER_CLAIM, remainingCustomer.toString());
    handler.handleRequest(refresh, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(ALLOWED_CUSTOMERS_CLAIM),
        is(equalTo(remainingCustomer.toString())));
    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM),
        is(equalTo(remainingCustomer.toString())));
  }

  @Test
  void shouldUseNamesFromLinkWhenRememberedPersonHasNoCustomer() {
    var username = FEDERATED_USERNAME_PREFIX + randomString();
    var cristinId = randomUri();
    var firstName = randomString();
    var lastName = randomString();
    identityService.saveLink(new IdentityLink(username, cristinId, null, null, Instant.now()));
    identityService.saveLinkedPerson(
        new LinkedPerson(cristinId, firstName, lastName, Set.of(), Instant.now()));
    var event = linkPathRefresh(username);

    handler.handleRequest(event, context);

    assertThat(extractClaimFromCognitoUpdateRequest(FIRST_NAME_CLAIM), is(equalTo(firstName)));
    assertThat(extractClaimFromCognitoUpdateRequest(LAST_NAME_CLAIM), is(equalTo(lastName)));
    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM), is(equalTo(EMPTY_CLAIM)));
    assertThat(
        extractClaimFromCognitoUpdateRequest(ALLOWED_CUSTOMERS_CLAIM), is(equalTo(EMPTY_CLAIM)));
  }

  @Test
  void shouldSkipRememberedCustomerThatCannotBeFetchedOnLinkPath() {
    var username = FEDERATED_USERNAME_PREFIX + randomString();
    var cristinId = randomUri();
    identityService.saveLink(new IdentityLink(username, cristinId, null, null, Instant.now()));
    identityService.saveLinkedPerson(
        new LinkedPerson(
            cristinId,
            randomString(),
            randomString(),
            Set.of(nonExistingCustomerId()),
            Instant.now()));
    var event = linkPathRefresh(username);

    handler.handleRequest(event, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM), is(equalTo(EMPTY_CLAIM)));
    assertThat(
        extractClaimFromCognitoUpdateRequest(ALLOWED_CUSTOMERS_CLAIM), is(equalTo(EMPTY_CLAIM)));
  }

  @Test
  void shouldGiveNoAccessToRememberedCustomerWithoutUserEntryOnLinkPath() {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var customerWithoutUserEntry = fetchCustomersWithActiveEmploymentsForPerson(nin).get(0);
    var username = FEDERATED_USERNAME_PREFIX + randomString();
    var cristinId = scenarios.getCristinIdForPerson(nin);
    identityService.saveLink(new IdentityLink(username, cristinId, null, null, Instant.now()));
    identityService.saveLinkedPerson(
        new LinkedPerson(
            cristinId,
            randomString(),
            randomString(),
            Set.of(customerWithoutUserEntry),
            Instant.now()));
    var event = linkPathRefresh(username);

    handler.handleRequest(event, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM), is(equalTo(EMPTY_CLAIM)));
    assertThat(
        extractClaimFromCognitoUpdateRequest(ALLOWED_CUSTOMERS_CLAIM), is(equalTo(EMPTY_CLAIM)));
  }

  /** A token refresh without an identity number for a user that only exists as link rows. */
  private CognitoUserPoolPreTokenGenerationEventV2 linkPathRefresh(String username) {
    var event = withoutNin(feideLogin(scenarios.personWithExactlyOneActiveEmployment().nin()));
    event.setUserName(username);
    return refreshFor(event);
  }

  @Test
  void shouldKeepSelectedCustomerWhenRefreshingThroughLink() {
    var nin = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    var login = newLoginEvent(nin, NON_FEIDE);
    handler.handleRequest(login, context);
    var selectedCustomer = fetchCustomersWithActiveEmploymentsForPerson(nin).iterator().next();

    var refresh = withoutNin(login);
    refresh
        .getRequest()
        .getUserAttributes()
        .put(CURRENT_CUSTOMER_CLAIM, selectedCustomer.toString());
    refresh.setTriggerSource(TRIGGER_SOURCE_REFRESH_TOKENS);
    handler.handleRequest(refresh, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM),
        is(equalTo(selectedCustomer.toString())));
  }

  @Test
  void shouldClearSelectedCustomerWithoutFailingWhenRefreshingThroughLinkAndCustomerIsNotAllowed() {
    var nin = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    var login = newLoginEvent(nin, NON_FEIDE);
    handler.handleRequest(login, context);

    var refresh = withoutNin(login);
    refresh.getRequest().getUserAttributes().put(CURRENT_CUSTOMER_CLAIM, randomUri().toString());
    refresh.setTriggerSource(TRIGGER_SOURCE_REFRESH_TOKENS);
    handler.handleRequest(refresh, context);

    assertThatCustomerSelectionClaimsAreCleared();
  }

  @Test
  void shouldFindUserEntryWithLegacyUsernameThroughIndexOnLinkPath() {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var login = feideLogin(nin);
    var feideIdentifier = extractFeideIdentifierFromInputEvent(login);
    var legacyUser =
        scenarios
            .createLegacyUsersForAllActiveAffiliations(nin, feideIdentifier, identityService)
            .stream()
            .collect(SingletonCollector.collect());
    handler.handleRequest(login, context);
    var refresh = refreshFor(withoutNin(login));
    refresh
        .getRequest()
        .getUserAttributes()
        .put(CURRENT_CUSTOMER_CLAIM, legacyUser.getInstitution().toString());

    handler.handleRequest(refresh, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(NVA_USERNAME_CLAIM),
        is(equalTo(legacyUser.getUsername())));
    assertThat(scanAllUsers(), hasSize(SINGLE_EXPECTED_USER));
  }

  @Test
  void shouldLogTriggerSourceForEveryInvocation() {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var event = newLoginEvent(nin, NON_FEIDE);
    var logRecorder = LogRecorder.forRoot(UserSelectionUponLoginHandlerTest.class);

    handler.handleRequest(event, context);

    Assertions.assertThat(logRecorder.messages())
        .anyMatch(message ->
            message.contains("Trigger source " + TRIGGER_SOURCE_HOSTED_AUTH)
                && message.contains(event.getUserName()));
  }

  /** The same user refreshing tokens, with the stored attributes as Cognito sends them. */
  private static CognitoUserPoolPreTokenGenerationEventV2 refreshFor(
      CognitoUserPoolPreTokenGenerationEventV2 login) {
    var event = new CognitoUserPoolPreTokenGenerationEventV2();
    event.setTriggerSource(TRIGGER_SOURCE_REFRESH_TOKENS);
    event.setUserName(login.getUserName());
    event.setUserPoolId(login.getUserPoolId());
    event.setRequest(
        Request.builder()
            .withUserAttributes(new HashMap<>(login.getRequest().getUserAttributes()))
            .build());
    return event;
  }

  /** The same user logging in again, as if the identity provider delivered no identity number. */
  private static CognitoUserPoolPreTokenGenerationEventV2 withoutNin(
      CognitoUserPoolPreTokenGenerationEventV2 original) {
    var attributes = new HashMap<>(original.getRequest().getUserAttributes());
    attributes.remove(NIN_FOR_FEIDE_USERS);
    attributes.remove(NIN_FOR_NON_FEIDE_USERS);
    var event = new CognitoUserPoolPreTokenGenerationEventV2();
    event.setTriggerSource(original.getTriggerSource());
    event.setUserName(original.getUserName());
    event.setUserPoolId(original.getUserPoolId());
    event.setRequest(Request.builder().withUserAttributes(attributes).build());
    return event;
  }

  private static CognitoUserPoolPreTokenGenerationEventV2 passwordLogin(URI cristinId, String nin) {
    Map<String, String> userAttributes = new HashMap<>();
    if (nonNull(cristinId)) {
      userAttributes.put(PERSON_CRISTIN_ID_CLAIM, cristinId.toString());
    }
    if (nonNull(nin)) {
      userAttributes.put(NIN_FOR_NON_FEIDE_USERS, nin);
    }
    userAttributes.put(NAME_CLAIM, EXAMPLE_NAME);
    var event = new CognitoUserPoolPreTokenGenerationEventV2();
    event.setTriggerSource(TRIGGER_SOURCE_AUTHENTICATION);
    event.setUserName(randomString());
    event.setRequest(Request.builder().withUserAttributes(userAttributes).build());
    return event;
  }

  @Test
  void shouldHandleFeideOrgRegardlessOfCase() throws NotFoundException {
    var mockPerson = scenarios.personWithExactlyOneActiveEmployment();

    var event = feideLogin(mockPerson.nin());
    var orgFeideDomain = extractFeideDomainFromInputEvent(event);
    var customer = customerService.getCustomerByOrgDomain(orgFeideDomain);
    event
        .getRequest()
        .getUserAttributes()
        .put(
            ORG_FEIDE_DOMAIN,
            event.getRequest().getUserAttributes().get(ORG_FEIDE_DOMAIN).toUpperCase());

    var result = handler.handleRequest(event, context);

    var currentCustomer =
        result
            .getResponse()
            .getClaimsAndScopeOverrideDetails()
            .getAccessTokenGeneration()
            .getClaimsToAddOrOverride()
            .get(CURRENT_CUSTOMER_CLAIM);
    assertThat(currentCustomer, containsString(customer.getId().toString()));
  }

  @ParameterizedTest(
      name =
          "should clear customer selection claims when user has many affiliations and logs in with"
              + "personal number")
  @EnumSource(
      value = LoginEventType.class,
      names = {"NON_FEIDE"},
      mode = Mode.INCLUDE)
  void shouldClearCustomerSelectionClaimsIdWhenUserHasManyAffiliationsAndLogsInWithPersonalNumber(
      LoginEventType loginEventType) {
    var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    var event = newLoginEvent(person, loginEventType);

    handler.handleRequest(event, context);

    assertThatCustomerSelectionClaimsAreCleared();
  }

  @ParameterizedTest(name = "should not clear customer selection claims when refreshing token")
  @EnumSource(
      value = LoginEventType.class,
      names = {"NON_FEIDE"},
      mode = Mode.INCLUDE)
  void shouldNotClearCustomerSelectionClaimsRefreshingToken(LoginEventType loginEventType) {
    var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    var event = newLoginEvent(person, loginEventType);
    handler.handleRequest(event, context);
    var userAttributes = new HashMap<>(event.getRequest().getUserAttributes());
    var customer = fetchCustomersWithActiveEmploymentsForPerson(person).stream().findFirst();
    userAttributes.put(CURRENT_CUSTOMER_CLAIM, customer.get().toString());
    event.getRequest().setUserAttributes(userAttributes);
    event.setTriggerSource(TRIGGER_SOURCE_REFRESH_TOKENS);
    handler.handleRequest(event, context);

    final var customerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
    assertThat(customerId, is(not(equalTo(EMPTY_CLAIM))));
  }

  @ParameterizedTest(name = "should not return allowed customers when no affiliations")
  @EnumSource(
      value = LoginEventType.class,
      names = {"NON_FEIDE"},
      mode = Mode.INCLUDE)
  void shouldNotReturnAllowedCustomersWhenNoAffiliations(LoginEventType loginEventType)
      throws NotFoundException {
    var person = scenarios.personWithoutAffiliations().nin();
    var cristinPersonId = mockPersonRegistry.getCristinIdForPerson(person);
    termsAndConditionsService.updateTermsAndConditions(cristinPersonId, TERMS_URI, randomString());
    var event = newLoginEvent(person, loginEventType);
    handler.handleRequest(event, context);
    event.setTriggerSource(TRIGGER_SOURCE_REFRESH_TOKENS);
    handler.handleRequest(event, context);

    final var customerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
    assertThat(customerId, is(equalTo(EMPTY_CLAIM)));

    final var allowedCustomers = extractClaimFromCognitoUpdateRequest(ALLOWED_CUSTOMERS_CLAIM);
    assertThat(allowedCustomers, is(equalTo(EMPTY_CLAIM)));
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

  @ParameterizedTest(
      name =
          "should not assign access rights for active employment when institution (top-level org) "
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

  private List<String> extractAccessRights(CognitoUserPoolPreTokenGenerationEventV2 response) {
    return Arrays.asList(
        response
            .getResponse()
            .getClaimsAndScopeOverrideDetails()
            .getGroupOverrideDetails()
            .getGroupsToOverride());
  }

  @ParameterizedTest(
      name =
          "should include all customers with active employments in the Cognito field "
              + "custom:allowedCustomers")
  @EnumSource(value = LoginEventType.class, names = "NON_FEIDE", mode = Mode.INCLUDE)
  void
      shouldIncludeAllCustomersWithActiveEmploymentsInCognitoFieldAllowedCustomersWhenLoggingInWithNonFeide(
          LoginEventType loginEventType) throws NotFoundException {

    var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    var usersWithRoles = createUsersWithRolesForPerson(person);
    var firstUser = usersWithRoles.stream().findFirst().get();
    termsAndConditionsService.updateTermsAndConditions(
        firstUser.getCristinId(), TERMS_URI, firstUser.getUsername());

    var expectedCustomerIds =
        scenarios.fetchCustomersForPerson(person).stream().map(CustomerDto::getId).toList();
    var event = newLoginEvent(person, loginEventType);
    handler.handleRequest(event, context);
    var actualAllowedCustomerIds = extractAllowedCustomersFromCongitoUpdateRequest();

    assertThat(
        actualAllowedCustomerIds, containsInAnyOrder(expectedCustomerIds.toArray(URI[]::new)));
  }

  private List<URI> extractAllowedCustomersFromCongitoUpdateRequest() {
    return cognitoClient.getAdminUpdateUserRequest().userAttributes().stream()
        .filter(attribute -> attribute.name().equals(ALLOWED_CUSTOMERS_CLAIM))
        .map(AttributeType::value)
        .map(this::extractAllowedCustomersRespectingEmptyStringNull)
        .flatMap(Arrays::stream)
        .collect(Collectors.toList());
  }

  private URI[] extractAllowedCustomersRespectingEmptyStringNull(String value) {
    if (EMPTY_CLAIM.equals(value)) {
      return new URI[] {};
    } else {
      return Arrays.stream(value.split(",")).map(URI::create).toList().toArray(URI[]::new);
    }
  }

  @ParameterizedTest(
      name =
          "should include only customer from feide in the Cognito field "
              + "custom:allowedCustomers")
  @EnumSource(value = LoginEventType.class, names = "FEIDE", mode = Mode.INCLUDE)
  void shouldIncludeOnlyFeideCustomerInCognitoFieldAllowedCustomersWhenLoggingInWithFeide(
      LoginEventType loginEventType) throws NotFoundException {

    var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    var usersWithRoles = createUsersWithRolesForPerson(person);

    var firstUser = usersWithRoles.stream().findFirst().get();
    termsAndConditionsService.updateTermsAndConditions(
        firstUser.getCristinId(), TERMS_URI, firstUser.getUsername());
    var event = newLoginEvent(person, loginEventType);
    handler.handleRequest(event, context);
    var orgFeideDomain = extractFeideDomainFromInputEvent(event);
    var currentCustomer = customerService.getCustomerByOrgDomain(orgFeideDomain);
    var actualAllowedCustomerIds = extractAllowedCustomersFromCongitoUpdateRequest();

    assertThat(actualAllowedCustomerIds, containsInAnyOrder(currentCustomer.getId()));
  }

  @ParameterizedTest(
      name =
          "should not include customers with inactive employments in the Cognito field "
              + "custom:allowedCustomers")
  @EnumSource(LoginEventType.class)
  void shouldNotIncludeCustomersWithInactiveEmploymentsInCognitoField(LoginEventType loginEventType)
      throws NotFoundException {
    var person =
        scenarios.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions().nin();
    var usersWithRoles = createUsersWithRolesForPerson(person);

    var firstUser = usersWithRoles.stream().findFirst().get();
    termsAndConditionsService.updateTermsAndConditions(
        firstUser.getCristinId(), TERMS_URI, firstUser.getUsername());

    var expectedCustomerIds = fetchCustomersWithActiveEmploymentsForPerson(person);
    assertThat(expectedCustomerIds, hasSize(1));
    var event = newLoginEvent(person, loginEventType);
    handler.handleRequest(event, context);
    var actualAllowedCustomerIds = extractAllowedCustomersFromCongitoUpdateRequest();
    assertThat(
        actualAllowedCustomerIds, containsInAnyOrder(expectedCustomerIds.toArray(URI[]::new)));
  }

  private List<URI> fetchCustomersWithActiveEmploymentsForPerson(String nin) {
    return scenarios.getCristinUriForInstitutionAffiliations(nin, ACTIVE).stream()
        .map(attempt(cristinId -> customerService.getCustomerByCristinId(cristinId)))
        .map(Try::orElseThrow)
        .map(CustomerDto::getId)
        .collect(Collectors.toList());
  }

  @Test
  void shouldStoreAllUserRolesForActiveTopLevelAffiliationInCognitoUserAttributesForFeide()
      throws NotFoundException {
    var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    var usersWithRoles = createUsersWithRolesForPerson(person);

    var firstUser = usersWithRoles.stream().findFirst().get();
    termsAndConditionsService.updateTermsAndConditions(
        firstUser.getCristinId(), TERMS_URI, firstUser.getUsername());

    var event = newLoginEvent(person, FEIDE);
    handler.handleRequest(event, context);

    var orgFeideDomain = extractFeideDomainFromInputEvent(event);
    var currentCustomer = customerService.getCustomerByOrgDomain(orgFeideDomain);

    var expectedRoles =
        usersWithRoles.stream()
            .filter(u -> u.getInstitution().equals(currentCustomer.getId()))
            .map(UserDto::getRoles)
            .flatMap(Collection::stream)
            .map(RoleDto::getRoleName)
            .map(RoleName::getValue)
            .collect(Collectors.toSet());

    var actualRoles =
        cognitoClient.getAdminUpdateUserRequest().userAttributes().stream()
            .filter(attribute -> attribute.name().equals(ROLES_CLAIM))
            .map(AttributeType::value)
            .map(str -> str.split(","))
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    assertThat(actualRoles, containsInAnyOrder(expectedRoles.toArray(String[]::new)));
  }

  private List<UserDto> createUsersWithRolesForPerson(String nin) {
    return scenarios.createUsersForAllActiveAffiliations(nin, identityService).stream()
        .map(attempt(user -> addRoleToUser(user, persistRandomRole())))
        .map(Try::orElseThrow)
        .collect(Collectors.toList());
  }

  private UserDto addRoleToUser(UserDto user, RoleDto persistRandomRole) throws NotFoundException {
    user.setRoles(Set.of(persistRandomRole));
    identityService.updateUser(user);
    return identityService.getUser(user);
  }

  private RoleDto persistRandomRole() throws InvalidInputException, ConflictException {
    var newRole =
        RoleDto.newBuilder()
            .withRoleName(randomRoleName())
            .withAccessRights(randomAccessRights())
            .build();
    persistRoleIfNotExist(newRole.copy().withRoleName(newRole.getRoleName()).build());
    return attempt(() -> identityService.getRole(newRole)).orElseThrow();
  }

  @Test
  void shouldStoreNoRolesInCognitoUserAttributesForNonFeide() {
    var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    createUsersWithRolesForPerson(person);

    var event = newLoginEvent(person, NON_FEIDE);
    handler.handleRequest(event, context);
    var actualRoles =
        cognitoClient.getAdminUpdateUserRequest().userAttributes().stream()
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

  @ParameterizedTest(
      name =
          "should store user's top-level-org affiliation in cognito user attributes when "
              + "user has only one active affiliation")
  @EnumSource(LoginEventType.class)
  void shouldStoreUsersTopLevelAffiliationWhenUserHasOnlyOneActiveAffiliation(
      LoginEventType loginEventType) {
    var person = scenarios.personWithExactlyOneActiveEmployment().nin();
    var personTopLevelOrgAffiliation =
        scenarios.getCristinUriForInstitutionAffiliations(person).stream()
            .collect(SingletonCollector.tryCollect())
            .orElseThrow();

    var event = newLoginEvent(person, loginEventType);
    handler.handleRequest(event, context);
    var actualTopOrgCristinId = extractClaimFromCognitoUpdateRequest(TOP_ORG_CRISTIN_ID);

    assertThat(URI.create(actualTopOrgCristinId), is(equalTo(personTopLevelOrgAffiliation)));
  }

  @ParameterizedTest(
      name =
          "should store user's top-level-org affiliation in cognito user attributes when user has"
              + " many active affiliations but logged in with Feide")
  @EnumSource(
      value = LoginEventType.class,
      names = {"FEIDE"},
      mode = Mode.INCLUDE)
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
    var person =
        scenarios.personWithTwoActiveEmploymentsInDifferentInstitutionsWithoutFeideDomain().nin();
    var role = persistRandomRole();
    createUsersWithRolesForPerson(person, role);
    addAccessRightToExistingRole(role, AccessRight.MANAGE_DEGREE);

    var event = newLoginEvent(person, loginEventType);
    handler.handleRequest(event, context);

    var actualAccessRights = extractClaimFromCognitoUpdateRequest(ACCESS_RIGHTS_CLAIM);
    assertThat(actualAccessRights, is(emptyString()));
  }

  private void addAccessRightToExistingRole(RoleDto role, AccessRight accessRight)
      throws InvalidInputException, NotFoundException {
    var accessRights = new HashSet<>(role.getAccessRights());
    accessRights.add(accessRight);
    var updatedRole =
        RoleDto.newBuilder()
            .withRoleName(role.getRoleName())
            .withAccessRights(accessRights)
            .build();
    updateRole(updatedRole);
  }

  private void updateRole(RoleDto updatedRole) throws InvalidInputException, NotFoundException {
    identityService.updateRole(updatedRole);
  }

  private List<UserDto> createUsersWithRolesForPerson(String nin, RoleDto role) {
    return scenarios.createUsersForAllActiveAffiliations(nin, identityService).stream()
        .map(attempt(user -> addRoleToUser(user, role)))
        .map(Try::orElseThrow)
        .collect(Collectors.toList());
  }

  @Test
  void
      shouldOnlyStoreAccessRightsInCognitoOfCurrentCustomerWhenUserHasSeveralActiveAffiliationsAndActiveCustomer()
          throws NotFoundException, InvalidInputException, ConflictException {
    var person = scenarios.personWithTwoActiveEmploymentsInDifferentInstitutions().nin();
    var role = persistRandomRole();
    var users = createUsersWithRolesForPerson(person, role);
    var firstUser = users.stream().findFirst().get();
    addAccessRightToExistingRole(role, AccessRight.MANAGE_DOI);
    termsAndConditionsService.updateTermsAndConditions(
        firstUser.getCristinId(), TERMS_URI, firstUser.getUsername());

    var event = newLoginEvent(person, FEIDE);

    handler.handleRequest(event, context);

    var actualAccessRights = extractClaimFromCognitoUpdateRequest(ACCESS_RIGHTS_CLAIM);
    var manageDoiAccessRights =
        Stream.of(actualAccessRights.split(ELEMENTS_DELIMITER))
            .filter(accessRight -> accessRight.equals(AccessRight.MANAGE_DOI.toPersistedString()))
            .toList();
    assertThat(manageDoiAccessRights, hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(LoginEventType.class)
  void shouldStoreAccessRightsInCognitoWhenUserHasOneActiveAffiliations(
      LoginEventType loginEventType)
      throws NotFoundException, InvalidInputException, ConflictException {
    var person = scenarios.personWithExactlyOneActiveEmployment().nin();

    var user =
        scenarios.createUsersForAllActiveAffiliations(person, identityService).stream()
            .collect(SingletonCollector.collect());
    var accessRight = randomElement(AccessRight.values());
    var role = persistRoleToDatabase(List.of(accessRight));
    assignExistingRoleToUser(user, role);
    addAccessRightToExistingRole(role, accessRight);
    termsAndConditionsService.updateTermsAndConditions(
        user.getCristinId(), TERMS_URI, user.getUsername());

    var event = newLoginEvent(person, loginEventType);
    handler.handleRequest(event, context);

    var actualAccessRights = extractClaimFromCognitoUpdateRequest(ACCESS_RIGHTS_CLAIM);
    assertThat(actualAccessRights, containsString(accessRight.toPersistedString()));
  }

  @ParameterizedTest(
      name =
          "should store user's username in cognito user attributes when user has "
              + "many active affiliations but logged in with Feide")
  @EnumSource(
      value = LoginEventType.class,
      names = {"FEIDE"},
      mode = Mode.INCLUDE)
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

  private String constructExpectedUsername(String nin, CustomerDto currentCustomer) {
    String personIdentifier = scenarios.getPersonFromRegistry(nin).getId();
    String topOrgCristinIdentifier =
        UriWrapper.fromUri(currentCustomer.getCristinId()).getLastPathElement();
    return String.join(AT, personIdentifier, topOrgCristinIdentifier);
  }

  @ParameterizedTest(
      name =
          "should store user's username in cognito user attributes when "
              + "user has only one active affiliation")
  @EnumSource(LoginEventType.class)
  void shouldStoreUsersUsernameWhenUserHasOnlyOneActiveAffiliation(LoginEventType loginEventType) {
    var person = scenarios.personWithExactlyOneActiveEmployment().nin();
    var event = newLoginEvent(person, loginEventType);
    handler.handleRequest(event, context);
    var currentCustomer =
        customerService.getCustomers().stream().collect(SingletonCollector.collect());
    var expectedUsername = constructExpectedUsername(person, currentCustomer);
    var actualUsername = extractClaimFromCognitoUpdateRequest(NVA_USERNAME_CLAIM);

    assertThat(actualUsername, is(equalTo(expectedUsername)));
  }

  @ParameterizedTest
  @EnumSource(LoginEventType.class)
  void shouldUpdateCognitoUserInfoDetailsWithCurrentUserAffiliation(LoginEventType loginEventType)
      throws NotFoundException {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var user =
        scenarios.createUsersForAllActiveAffiliations(person.nin(), identityService).stream()
            .collect(SingletonCollector.collect());
    var event = newLoginEvent(person.nin(), loginEventType);
    termsAndConditionsService.updateTermsAndConditions(
        user.getCristinId(), TERMS_URI, user.getUsername());

    handler.handleRequest(event, context);
    var expectedAffiliation =
        scenarios.getCristinUriForUnitAffiliations(person.nin()).stream()
            .collect(SingletonCollector.collect());
    var cognitoAttribute = extractClaimFromCognitoUpdateRequest(PERSON_AFFILIATION_CLAIM);
    assertThat(cognitoAttribute, is(equalTo(expectedAffiliation.toString())));
  }

  @Test
  void shouldUpdateUsersAccessRightsWhenRoleHasNewAccessRight()
      throws InvalidInputException, ConflictException, NotFoundException {
    var person = scenarios.personWithExactlyOneActiveEmployment().nin();
    var approveDoiAccessRight = AccessRight.MANAGE_DOI;
    var approvePublishAccessRight = AccessRight.MANAGE_PUBLISHING_REQUESTS;

    var user =
        scenarios.createUsersForAllActiveAffiliations(person, identityService).stream()
            .collect(SingletonCollector.collect());
    var role = persistRoleToDatabase(List.of(approveDoiAccessRight));
    assignExistingRoleToUser(user, role);

    addAccessRightToExistingRole(role, approvePublishAccessRight);

    termsAndConditionsService.updateTermsAndConditions(
        user.getCristinId(), TERMS_URI, user.getUsername());

    var event = newLoginEvent(person, LoginEventType.FEIDE);

    var response = handler.handleRequest(event, context);

    assertIdTokenContainsGroupAccessRightClaims(
        user, response, approveDoiAccessRight, approvePublishAccessRight);

    assertUserAccessRightsAreUpdated(user, approveDoiAccessRight, approvePublishAccessRight);
  }

  @Test
  void shouldUpdateViewingScopeClaimsUponLogin() throws NotFoundException {
    var person = scenarios.personWithExactlyOneActiveEmployment().nin();
    var user =
        scenarios.createUsersForAllActiveAffiliations(person, identityService).stream()
            .collect(SingletonCollector.collect());

    termsAndConditionsService.updateTermsAndConditions(
        user.getCristinId(), TERMS_URI, user.getUsername());

    var event = newLoginEvent(person, LoginEventType.FEIDE);

    handler.handleRequest(event, context);

    assertThatViewingScopeClaimsArePresent(new String[] {"1", "2"}, new String[] {"3", "4"});
  }

  private void assertThatViewingScopeClaimsArePresent(
      String[] expectedViewingScopeIncluded, String[] expectedViewingScopeExcluded) {
    final var viewingScopeIncluded =
        extractClaimFromCognitoUpdateRequest(VIEWING_SCOPE_INCLUDED_CLAIM);
    assertCommaSeparatedClaimContainsInAnyOrder(
        VIEWING_SCOPE_INCLUDED_CLAIM, viewingScopeIncluded, expectedViewingScopeIncluded);

    final var viewingScopeExcluded =
        extractClaimFromCognitoUpdateRequest(VIEWING_SCOPE_EXCLUDED_CLAIM);
    assertCommaSeparatedClaimContainsInAnyOrder(
        VIEWING_SCOPE_EXCLUDED_CLAIM, viewingScopeExcluded, expectedViewingScopeExcluded);
  }

  private void assertCommaSeparatedClaimContainsInAnyOrder(
      String claimName, String actual, String[] expected) {
    Set<String> actualAsSet;
    if (actual == null || EMPTY_CLAIM.equals(actual)) {
      actualAsSet = Collections.emptySet();
    } else {
      actualAsSet = new HashSet<>(Arrays.asList(actual.split(COMMA)));
    }
    assertThat(
        claimName + " should contain comma separated values in any order",
        actualAsSet,
        containsInAnyOrder(expected));
  }

  private void assertUserAccessRightsAreUpdated(UserDto user, AccessRight... accessRights)
      throws NotFoundException {
    var updatedUser = identityService.getUser(user);
    assertThat(updatedUser.getAccessRights(), containsInAnyOrder(accessRights));
  }

  private void assertIdTokenContainsGroupAccessRightClaims(
      UserDto user,
      CognitoUserPoolPreTokenGenerationEventV2 response,
      AccessRight... accessRights) {
    var actualAccessRightClaims = extractAccessRights(response);
    var expectedAccessRightClaims =
        constructExpectedAccessRightsForGroup(user, Set.of(accessRights)).toArray();
    assertThat(actualAccessRightClaims, containsInAnyOrder(expectedAccessRightClaims));
  }

  @Test
  void shouldSetCustomerSelectionClaimsOnFeideLoginWithEmploymentInBothFeideAndNonFeideCustomers() {

    var person = scenarios.personWithTwoActiveEmploymentsInNonFeideAndFeideCustomers().nin();
    var event = newLoginEvent(person, LoginEventType.FEIDE);

    var selectedCustomer =
        scenarios.fetchCustomersForPerson(person).stream()
            .filter(customer -> Objects.nonNull(customer.getFeideOrganizationDomain()))
            .collect(SingletonCollector.collect());

    handler.handleRequest(event, context);

    var expectedCustomerId = selectedCustomer.getId();
    var expectedCristinId = selectedCustomer.getCristinId();
    var expectedUsername = constructExpectedUsername(person, selectedCustomer);

    assertThatCustomerSelectionClaimsArePresent(
        expectedCustomerId, expectedCristinId, expectedUsername);
  }

  private void assertThatCustomerSelectionClaimsArePresent(
      URI expectedCustomerId, URI expectedCristinId, String expectedUsername) {
    final var customerId = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);
    assertThat(customerId, is(equalTo(expectedCustomerId.toString())));

    final var topOrgCristinId = extractClaimFromCognitoUpdateRequest(TOP_ORG_CRISTIN_ID);
    assertThat(topOrgCristinId, is(equalTo(expectedCristinId.toString())));

    final var nvaUsername = extractClaimFromCognitoUpdateRequest(NVA_USERNAME_CLAIM);
    assertThat(nvaUsername, is(equalTo(expectedUsername)));

    final var personAffiliation = extractClaimFromCognitoUpdateRequest(PERSON_AFFILIATION_CLAIM);
    assertThat(personAffiliation, is(notNullValue()));
  }

  @Test
  void shouldSetClaimsForBackendClientWhenClientCredentialsIsTriggerSource() {
    var client =
        ClientDto.newBuilder()
            .withClientId(randomString())
            .withCustomer(randomUri())
            .withCristinOrgUri(randomUri())
            .withActingUser(randomString())
            .build();
    identityService.addExternalClient(client);

    var response = handler.handleRequest(clientCredentialsEvent(client.getClientId()), context);

    var accessTokenClaims =
        response
            .getResponse()
            .getClaimsAndScopeOverrideDetails()
            .getAccessTokenGeneration()
            .getClaimsToAddOrOverride();
    assertThat(
        accessTokenClaims, hasEntry(CURRENT_CUSTOMER_CLAIM, client.getCustomer().toString()));
  }

  private static CognitoUserPoolPreTokenGenerationEventV2 clientCredentialsEvent(String clientId) {
    var event = new CognitoUserPoolPreTokenGenerationEventV2();
    event.setTriggerSource(TRIGGER_SOURCE_CLIENT_CREDENTIALS);
    event.setCallerContext(CallerContext.builder().withClientId(clientId).build());
    return event;
  }

  /** A customer id of the right shape that no customer has, as a deleted customer leaves behind. */
  private static URI nonExistingCustomerId() {
    return URI.create("https://www.example.org/namespace/" + java.util.UUID.randomUUID());
  }

  @Test
  void shouldFailTheRefreshWhenARememberedCustomerCannotBeRead() throws NotFoundException {
    var login = feideLogin(scenarios.personWithExactlyOneActiveEmployment().nin());
    handler.handleRequest(login, context);
    var failingCustomerService = spy(customerService);
    doThrow(new RuntimeException("Table unavailable"))
        .when(failingCustomerService)
        .getCustomer(any(URI.class));
    handler =
        new UserSelectionUponLoginHandler(
            cognitoClient, failingCustomerService, identityService, termsAndConditionsService);
    var refresh = refreshFor(login);

    assertThrows(RuntimeException.class, () -> handler.handleRequest(refresh, context));
  }

  @Test
  void shouldNotWriteTheIdentityNumberAttributes() {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();

    handler.handleRequest(newLoginEvent(nin, FEIDE), context);

    Assertions.assertThat(cognitoClient.getAdminUpdateUserRequest().userAttributes())
        .extracting(AttributeType::name)
        .doesNotContainAnyElementsOf(NIN_ATTRIBUTES);
  }

  @Test
  void shouldResolveLoginWithoutNinFromLink() {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var login = feideLogin(nin);
    handler.handleRequest(login, context);
    var customerFromRegistry = extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM);

    handler.handleRequest(withoutNin(login), context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(CURRENT_CUSTOMER_CLAIM),
        is(equalTo(customerFromRegistry)));
  }

  @Test
  void shouldResolveLoginFromRowsWhateverNumberTheUserStillCarries() {
    var login = feideLogin(scenarios.personWithExactlyOneActiveEmployment().nin());
    handler.handleRequest(login, context);
    var otherNin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var otherCristinId = scenarios.getCristinIdForPerson(otherNin);
    var otherUser =
        scenarios.createUsersForAllActiveAffiliations(otherNin, identityService).stream()
            .collect(SingletonCollector.collect());
    identityService.saveLinkedPerson(
        new LinkedPerson(
            otherCristinId,
            randomString(),
            randomString(),
            Set.of(otherUser.getInstitution()),
            Instant.now()));
    identityService.saveLink(
        new IdentityLink(login.getUserName(), otherCristinId, null, null, Instant.now()));
    handler = handlerAlone();

    handler.handleRequest(login, context);

    assertThat(
        extractClaimFromCognitoUpdateRequest(PERSON_CRISTIN_ID_CLAIM),
        is(equalTo(otherCristinId.toString())));
  }

  @Test
  void shouldFailWhenThePersonHasTwoUserEntriesAtTheSelectedCustomer() throws ConflictException {
    var nin = scenarios.personWithExactlyOneActiveEmployment().nin();
    var user =
        scenarios.createUsersForAllActiveAffiliations(nin, identityService).stream()
            .collect(SingletonCollector.collect());
    identityService.addUser(user.copy().withUsername(randomString()).build());
    var login = passwordLogin(scenarios.getCristinIdForPerson(nin), null);

    var exception =
        assertThrows(IllegalStateException.class, () -> handler.handleRequest(login, context));

    assertThat(
        exception.getMessage(),
        containsString(UserSelectionUponLoginHandler.COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR));
  }

  @Test
  void shouldRefuseAFederatedUserWithoutTheRowsTheInboundTriggerWrites() {
    var login = withoutNin(feideLogin(scenarios.personWithExactlyOneActiveEmployment().nin()));

    var exception =
        assertThrows(
            IdentityServiceMissingLinkException.class, () -> handler.handleRequest(login, context));

    assertThat(exception.getErrorCode(), is(equalTo("1004")));
  }
}
