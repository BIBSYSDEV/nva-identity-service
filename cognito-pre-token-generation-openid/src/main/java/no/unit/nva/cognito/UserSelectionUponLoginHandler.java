package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_INCLUDED_IN_ACCESS_TOKEN;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.FEIDE_ID;
import static no.unit.nva.cognito.CognitoClaims.IDENTITIES_ATTRIBUTE;
import static no.unit.nva.cognito.CognitoClaims.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import static no.unit.nva.database.IdentityService.defaultIdentityService;
import static nva.commons.core.StringUtils.isBlank;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.AccessTokenGeneration;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.ClaimsAndScopeOverrideDetails;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.GroupOverrideDetails;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.IdTokenGeneration;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.Response;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.TermsAndConditionsService;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.ActiveCustomers;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceMissingLinkException;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

/**
 * Builds the token claims for a user. Two ways to find out who the user is:
 *
 * <ul>
 *   <li>A federated user is resolved from the link and person rows that the inbound federation
 *       trigger wrote when the user logged in. This holds for a login and for a token refresh.
 *   <li>A password user is resolved from the administrator-set person identifier on the Cognito
 *       user.
 * </ul>
 *
 * <p>No call is made to the person registry.
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class UserSelectionUponLoginHandler
    implements RequestHandler<
        CognitoUserPoolPreTokenGenerationEventV2, CognitoUserPoolPreTokenGenerationEventV2> {

  public static final Environment ENVIRONMENT = new Environment();
  public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));
  public static final String ORG_FEIDE_DOMAIN = "custom:orgFeideDomain";
  public static final String COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR =
      "Could not find user for customer: ";
  public static final String TRIGGER_SOURCE_MESSAGE = "Trigger source {} for user {}";

  private static final Logger LOGGER = LoggerFactory.getLogger(UserSelectionUponLoginHandler.class);
  private static final String NO_USER_ENTRY_MESSAGE = "No user entry for person {} at customer {}";
  public static final String TRIGGER_SOURCE_REFRESH_TOKENS = "TokenGeneration_RefreshTokens";
  public static final String TRIGGER_SOURCE_CLIENT_CREDENTIALS =
      "TokenGeneration_ClientCredentials";
  private static final String EMPTY_NAME = "";
  private final CustomerService customerService;
  private final CognitoIdentityProviderClient cognitoClient;
  private final IdentityService identityService;
  private final TermsAndConditionsService termsService;
  private final M2MTokenEnricher tokenEnricher;

  @JacocoGenerated
  public UserSelectionUponLoginHandler() {
    this(
        defaultCognitoClient(),
        defaultCustomerService(DEFAULT_DYNAMO_CLIENT),
        defaultIdentityService(DEFAULT_DYNAMO_CLIENT),
        new TermsAndConditionsService());
  }

  @JacocoGenerated
  private static CognitoIdentityProviderClient defaultCognitoClient() {
    return CognitoIdentityProviderClient.builder()
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .httpClient(UrlConnectionHttpClient.create())
        .region(AWS_REGION)
        .build();
  }

  public UserSelectionUponLoginHandler(
      CognitoIdentityProviderClient cognitoClient,
      CustomerService customerService,
      IdentityService identityService,
      TermsAndConditionsService termsService) {

    this.cognitoClient = cognitoClient;
    this.customerService = customerService;
    this.identityService = identityService;
    this.tokenEnricher = new M2MTokenEnricher(identityService);
    this.termsService = termsService;
  }

  private static Optional<String> nonBlank(String value) {
    return Optional.ofNullable(value).filter(candidate -> !isBlank(candidate));
  }

  private static String extractOrgFeideDomain(Map<String, String> userAttributes) {
    return userAttributes.get(ORG_FEIDE_DOMAIN);
  }

  private static String extractFeideIdentifier(Map<String, String> userAttributes) {
    return userAttributes.get(FEIDE_ID);
  }

  private static void logIfDebug(String s, Instant start) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(s, Instant.now().toEpochMilli() - start.toEpochMilli());
    }
  }

  @Override
  public CognitoUserPoolPreTokenGenerationEventV2 handleRequest(
      CognitoUserPoolPreTokenGenerationEventV2 input, Context context) {
    try {
      return isBackendClient(input) ? tokenEnricher.enrichAccessToken(input) : processInput(input);
    } catch (Exception e) {
      LOGGER.error("Failed to process input due to", e);
      throw e;
    }
  }

  private static boolean isBackendClient(CognitoUserPoolPreTokenGenerationEventV2 input) {
    return TRIGGER_SOURCE_CLIENT_CREDENTIALS.equals(input.getTriggerSource());
  }

  private CognitoUserPoolPreTokenGenerationEventV2 processInput(
      CognitoUserPoolPreTokenGenerationEventV2 input) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Entering request handler...");
    }

    final var start = Instant.now();
    // TODO: Added to see which path each invocation takes while the link path rolls out. Reduce to
    // debug or remove once it has run in production for a while.
    LOGGER.info(TRIGGER_SOURCE_MESSAGE, input.getTriggerSource(), input.getUserName());

    var attributes = input.getRequest().getUserAttributes();
    var resolved = resolvePerson(extractAuthenticationDetails(input), attributes);
    var person = resolved.person();
    var customersForPerson = resolved.customers();
    var users = resolved.users();
    var authenticationDetails = resolved.authenticationDetails();

    var currentCustomer =
        getCurrentCustomer(
            authenticationDetails, customersForPerson, input.getTriggerSource(), attributes);

    var currentTerms = termsService.getCurrentTermsAndConditions().termsConditionsUri();
    var acceptedTerms = getAcceptedTerms(person);
    var hasAcceptedTerms = currentTerms.equals(acceptedTerms);

    var currentUser = getCurrentUser(currentCustomer, users);

    var userAccessRights =
        createAccessRightForCustomer(users, customersForPerson, currentCustomer, hasAcceptedTerms);

    var accessRightsPersistedFormat =
        userAccessRights.stream()
            .map(UserAccessRightForCustomer::getAccessRight)
            .map(AccessRight::toPersistedString)
            .toList();
    var accessRightsResponseStrings =
        userAccessRights.stream().map(UserAccessRightForCustomer::toString).toList();

    List<AttributeType> userAttributes =
        new UserAttributesBuilder()
            .withPerson(person)
            .withCurrentTerms(currentTerms)
            .withAcceptedTerms(acceptedTerms)
            .withUsers(users)
            .withCustomersForPerson(customersForPerson)
            .withCurrentCustomer(currentCustomer)
            .withAuthenticationDetails(authenticationDetails)
            .withCurrentUser(currentUser)
            .withAccessRightsPersistedFormat(accessRightsPersistedFormat)
            .build();

    updateCognitoUserAttributes(
        userAttributes, authenticationDetails.getUserPoolId(), authenticationDetails.getUsername());

    input.setResponse(
        Response.builder()
            .withClaimsAndScopeOverrideDetails(
                buildOverrideClaims(accessRightsResponseStrings, userAttributes))
            .build());
    logIfDebug("Leaving request handler having spent {} ms.", start);
    return input;
  }

  /** The person behind the login, with their active customers and user entries. */
  private record ResolvedPerson(
      Person person,
      Set<CustomerDto> customers,
      List<UserDto> users,
      AuthenticationDetails authenticationDetails) {}

  /**
   * A federated user is resolved from the link the inbound federation trigger wrote at login, and a
   * token refresh uses the same rows. A password user has no link; an administrator has set its
   * cristinId claim.
   */
  private ResolvedPerson resolvePerson(
      AuthenticationDetails authenticationDetails, Map<String, String> attributes) {
    return authenticationDetails.isFederated()
        ? resolveFromLink(authenticationDetails)
        : resolveFromCristinIdClaim(authenticationDetails, attributes);
  }

  /**
   * The inbound federation trigger writes the link and person rows before this handler runs, so a
   * federated user without them is a data error and is refused.
   */
  private ResolvedPerson resolveFromLink(AuthenticationDetails authenticationDetails) {
    var identity =
        identityService
            .fetchLinkedIdentity(authenticationDetails.getUsername())
            .orElseThrow(IdentityServiceMissingLinkException::new);
    return resolveFromLinkedPerson(authenticationDetails, identity.link(), identity.person());
  }

  private ResolvedPerson resolveFromLinkedPerson(
      AuthenticationDetails authenticationDetails, IdentityLink link, LinkedPerson linkedPerson) {
    var person = toPerson(linkedPerson);
    var rememberedCustomers = fetchActiveCustomers(linkedPerson.customers(), person);
    var users = fetchUsersForCustomers(person, rememberedCustomers);
    var customers = customersWithUserEntry(rememberedCustomers, users);
    return new ResolvedPerson(
        person, customers, users, authenticationDetails.withFeideDetailsFrom(link));
  }

  private ResolvedPerson resolveFromCristinIdClaim(
      AuthenticationDetails authenticationDetails, Map<String, String> attributes) {
    var cristinId =
        nonBlank(attributes.get(PERSON_CRISTIN_ID_CLAIM))
            .flatMap(value -> attempt(() -> URI.create(value)).toOptional())
            .orElseThrow(IdentityServiceMissingLinkException::new);
    var users = identityService.getUsersByCristinId(cristinId);
    var person = toPerson(cristinId, users);
    var customerIds = users.stream().map(UserDto::getInstitution).collect(Collectors.toSet());
    var customers = fetchActiveCustomers(customerIds, person);
    return new ResolvedPerson(person, customers, users, authenticationDetails);
  }

  private static Person toPerson(LinkedPerson linkedPerson) {
    return new Person(
        linkedPerson.personId(),
        personIdentifierOf(linkedPerson.personId()),
        linkedPerson.givenName(),
        linkedPerson.familyName(),
        List.of());
  }

  private static Person toPerson(URI cristinId, List<UserDto> users) {
    var someUser = users.stream().findFirst();
    return new Person(
        cristinId,
        personIdentifierOf(cristinId),
        someUser.map(UserDto::getGivenName).orElse(EMPTY_NAME),
        someUser.map(UserDto::getFamilyName).orElse(EMPTY_NAME),
        List.of());
  }

  private static String personIdentifierOf(URI cristinId) {
    return UriWrapper.fromUri(cristinId).getLastPathElement();
  }

  private Set<CustomerDto> fetchActiveCustomers(Set<URI> customerIds, Person person) {
    return ActiveCustomers.byIds(customerIds, customerService, person.getId());
  }

  private List<UserDto> fetchUsersForCustomers(Person person, Set<CustomerDto> customers) {
    return customers.stream()
        .map(customer -> fetchUserForCustomer(person, customer))
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * A remembered customer without a user entry gives no access. The inbound federation trigger
   * recreates the entry at the next login.
   */
  private static Set<CustomerDto> customersWithUserEntry(
      Set<CustomerDto> customers, List<UserDto> users) {
    var institutions = users.stream().map(UserDto::getInstitution).collect(Collectors.toSet());
    return customers.stream()
        .filter(customer -> institutions.contains(customer.getId()))
        .collect(Collectors.toSet());
  }

  /**
   * User entries are keyed on the person and customer identifiers, so the entry can be read by key
   * with a consistent read. Entries created before that scheme keep the Feide identifier as
   * username, so those are found through the index on the same identifiers.
   */
  private Optional<UserDto> fetchUserForCustomer(Person person, CustomerDto customer) {
    var username =
        UserEntriesCreatorForPerson.usernameFor(person.getIdentifier(), customer.getCristinId());
    return identityService
        .fetchUserConsistently(username)
        .or(() -> fetchUserByCristinIdentifiers(person, customer));
  }

  private Optional<UserDto> fetchUserByCristinIdentifiers(Person person, CustomerDto customer) {
    return attempt(() ->
            identityService.getUserByPersonCristinIdAndCustomerCristinId(
                person.getId(), customer.getCristinId()))
        .toOptional(fail ->
            LOGGER.warn(
                NO_USER_ENTRY_MESSAGE, person.getId(), customer.getId(), fail.getException()));
  }

  private CustomerDto getCurrentCustomer(
      AuthenticationDetails authenticationDetails,
      Set<CustomerDto> customersForPerson,
      String triggerSource,
      Map<String, String> userAttributes) {
    var currentCustomer =
        returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer(
            authenticationDetails.getFeideDomain(), customersForPerson);

    if (TRIGGER_SOURCE_REFRESH_TOKENS.equals(triggerSource)) {
      // If the user is refreshing tokens, the current customer will be set to the previously
      // selected customer
      currentCustomer =
          customersForPerson.stream()
              .filter(customer ->
                  customer
                      .getId()
                      .equals(
                          Optional.ofNullable(
                                  userAttributes.getOrDefault(CURRENT_CUSTOMER_CLAIM, null))
                              .map(URI::create)
                              .orElse(null)))
              .collect(SingletonCollector.tryCollect())
              .orElse(fail -> null);
    }

    return currentCustomer;
  }

  private URI getAcceptedTerms(Person person) {
    return Optional.ofNullable(termsService.getTermsAndConditionsByPerson(person.getId()))
        .map(TermsConditionsResponse::termsConditionsUri)
        .orElse(null);
  }

  private static AuthenticationDetails extractAuthenticationDetails(
      CognitoUserPoolPreTokenGenerationEventV2 input) {
    var attributes = input.getRequest().getUserAttributes();
    return new AuthenticationDetails(
        extractFeideIdentifier(attributes),
        extractOrgFeideDomain(attributes),
        input.getUserPoolId(),
        input.getUserName(),
        attributes.containsKey(IDENTITIES_ATTRIBUTE));
  }

  private UserDto getCurrentUser(CustomerDto currentCustomer, Collection<UserDto> users) {
    if (isNull(currentCustomer)) {
      return null;
    }

    var currentCustomerId = currentCustomer.getId();
    return attempt(() -> filterOutUser(users, currentCustomerId))
        .orElseThrow(fail -> handleUserNotFoundError(currentCustomerId));
  }

  private IllegalStateException handleUserNotFoundError(URI currentCustomerId) {
    return new IllegalStateException(COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR + currentCustomerId);
  }

  private UserDto filterOutUser(Collection<UserDto> users, URI currentCustomerId) {
    return users.stream()
        .filter(user -> user.getInstitution().equals(currentCustomerId))
        .collect(SingletonCollector.collect());
  }

  private CustomerDto
      returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer(
          String feideDomain, Set<CustomerDto> customers) {

    return customers.stream()
        .filter(customer -> selectFeideOrgIfApplicable(customer, feideDomain))
        .collect(SingletonCollector.tryCollect())
        .orElse(fail -> null);
  }

  private boolean selectFeideOrgIfApplicable(CustomerDto customer, String feideDomain) {
    return feideDomain == null
        || feideDomain.equalsIgnoreCase(customer.getFeideOrganizationDomain());
  }

  private void updateCognitoUserAttributes(
      Collection<AttributeType> userAttributes, String userPoolId, String username) {
    final var updateUserAttributesRequest =
        createUpdateUserAttributesRequest(userAttributes, userPoolId, username);
    cognitoClient.adminUpdateUserAttributes(updateUserAttributesRequest);
  }

  private AdminUpdateUserAttributesRequest createUpdateUserAttributesRequest(
      Collection<AttributeType> userAttributes, String userPoolId, String username) {

    return AdminUpdateUserAttributesRequest.builder()
        .userPoolId(userPoolId)
        .username(username)
        .userAttributes(userAttributes)
        .build();
  }

  private List<UserAccessRightForCustomer> createAccessRightForCustomer(
      List<UserDto> personUsers,
      Set<CustomerDto> customers,
      CustomerDto currentCustomer,
      boolean hasAcceptedTerms) {
    return personUsers.stream()
        .map(user -> UserAccessRightForCustomer.fromUser(user, customers, hasAcceptedTerms))
        .flatMap(Collection::stream)
        .filter(ac -> ac.getCustomer().equals(currentCustomer))
        .toList();
  }

  private ClaimsAndScopeOverrideDetails buildOverrideClaims(
      List<String> groupsToOverride, List<AttributeType> userAttributes) {
    var groups =
        GroupOverrideDetails.builder()
            .withGroupsToOverride(groupsToOverride.toArray(String[]::new))
            .build();

    return ClaimsAndScopeOverrideDetails.builder()
        .withGroupOverrideDetails(groups)
        .withAccessTokenGeneration(buildAccessTokenGeneration(userAttributes))
        .withIdTokenGeneration(buildIdTokenGeneration(userAttributes))
        .build();
  }

  private IdTokenGeneration buildIdTokenGeneration(Collection<AttributeType> userAttributes) {
    var excludedClaims =
        Stream.concat(
                Arrays.stream(CLAIMS_TO_BE_INCLUDED_IN_ACCESS_TOKEN),
                Arrays.stream(CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC))
            .toList();

    var claims =
        userAttributes.stream()
            .filter(attribute -> shouldBeIncludedExcept(attribute, excludedClaims))
            .collect(Collectors.toMap(AttributeType::name, AttributeType::value));

    return IdTokenGeneration.builder()
        .withClaimsToAddOrOverride(claims)
        .withClaimsToSuppress(excludedClaims.toArray(String[]::new))
        .build();
  }

  private static boolean shouldBeIncludedExcept(AttributeType a, List<String> excludedClaims) {
    return !excludedClaims.contains(a.name()) && nonNull(a.value()) && !a.value().isEmpty();
  }

  private static boolean shouldBeIncluded(AttributeType a, List<String> includedClaims) {
    return includedClaims.contains(a.name()) && nonNull(a.value()) && !a.value().isEmpty();
  }

  private AccessTokenGeneration buildAccessTokenGeneration(
      Collection<AttributeType> userAttributes) {
    var includedClaims = Arrays.asList(CLAIMS_TO_BE_INCLUDED_IN_ACCESS_TOKEN);
    var claims =
        userAttributes.stream()
            .filter(attribute -> shouldBeIncluded(attribute, includedClaims))
            .collect(Collectors.toMap(AttributeType::name, AttributeType::value));

    return AccessTokenGeneration.builder().withClaimsToAddOrOverride(claims).build();
  }
}
