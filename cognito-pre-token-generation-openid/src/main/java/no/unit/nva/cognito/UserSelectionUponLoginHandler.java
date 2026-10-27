package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_INCLUDED_IN_ACCESS_TOKEN;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.FEIDE_ID;
import static no.unit.nva.cognito.CognitoClaims.IDENTITIES_ATTRIBUTE;
import static no.unit.nva.cognito.CognitoClaims.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.cognito.CognitoClaims.NIN_FOR_NON_FEIDE_USERS;
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
import no.unit.nva.useraccessservice.usercreation.UserCreationContext;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonName;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceMissingLinkException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceMissingNinException;
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
 *   <li>The person registry path, for a federated login that delivered a national identity number.
 *       The person is looked up in the registry, user entries are created or updated, and what the
 *       registry answered is remembered as link and person rows.
 *   <li>The link path, for everything else. A token refresh, and a login without the number, are
 *       resolved from the link row written at an earlier login. Password users are resolved from
 *       the administrator-set person identifier on the Cognito user. No registry call is made.
 * </ul>
 */
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.GodClass"})
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
  private static final String LINK_WITHOUT_PERSON_MESSAGE =
      "Link for user {} points to person {} without a row, resolving through the registry";
  private static final String COULD_NOT_WRITE_ROWS_MESSAGE =
      "Could not write link and person rows for person {}, the login continues without them";
  private static final String COULD_NOT_EXTRACT_NAME_MESSAGE =
      "Could not extract name from user attributes with keys {}";
  public static final String TRIGGER_SOURCE_REFRESH_TOKENS = "TokenGeneration_RefreshTokens";
  public static final String TRIGGER_SOURCE_CLIENT_CREDENTIALS =
      "TokenGeneration_ClientCredentials";
  private static final String EMPTY_NAME = "";
  private final CustomerService customerService;
  private final CognitoIdentityProviderClient cognitoClient;
  private final IdentityService identityService;
  private final UserEntriesCreatorForPerson userCreator;
  private final PersonRegistry personRegistry;
  private final TermsAndConditionsService termsService;
  private final M2MTokenEnricher tokenEnricher;

  @JacocoGenerated
  public UserSelectionUponLoginHandler() {
    this.identityService = defaultIdentityService(DEFAULT_DYNAMO_CLIENT);
    this.cognitoClient = defaultCognitoClient();
    this.customerService = defaultCustomerService(DEFAULT_DYNAMO_CLIENT);
    this.userCreator = new UserEntriesCreatorForPerson(identityService);
    this.tokenEnricher = new M2MTokenEnricher(identityService);
    this.termsService = new TermsAndConditionsService();
    this.personRegistry = CristinPersonRegistry.defaultPersonRegistry();
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
      PersonRegistry personRegistry,
      TermsAndConditionsService termsService) {

    this.cognitoClient = cognitoClient;
    this.customerService = customerService;
    this.identityService = identityService;
    this.personRegistry = personRegistry;
    this.userCreator = new UserEntriesCreatorForPerson(identityService);
    this.tokenEnricher = new M2MTokenEnricher(identityService);
    this.termsService = termsService;
  }

  private static Optional<NationalIdentityNumber> extractNin(Map<String, String> userAttributes) {
    return nonBlank(userAttributes.get(NIN_FOR_FEIDE_USERS))
        .or(() -> nonBlank(userAttributes.get(NIN_FOR_NON_FEIDE_USERS)))
        .map(NationalIdentityNumber::fromString);
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
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
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
    var resolved =
        resolvePerson(extractAuthenticationDetails(input), attributes, input.getTriggerSource());
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
   * A federated login must deliver a national identity number and goes to the person registry, so a
   * reused Feide account or a merged person is caught at every login. A token refresh uses the rows
   * written at that login; a refresh without usable rows goes to the registry like a login. A
   * password user has no number and no link; an administrator has set its cristinId claim.
   */
  private ResolvedPerson resolvePerson(
      AuthenticationDetails authenticationDetails,
      Map<String, String> attributes,
      String triggerSource) {
    if (!authenticationDetails.isFederated()) {
      return resolveFromCristinIdClaim(authenticationDetails, attributes);
    }
    if (TRIGGER_SOURCE_REFRESH_TOKENS.equals(triggerSource)) {
      var resolvedFromLink = resolveFromLink(authenticationDetails);
      if (resolvedFromLink.isPresent()) {
        return resolvedFromLink.get();
      }
    }
    if (authenticationDetails.getNin().isEmpty()) {
      throw new IdentityServiceMissingNinException();
    }
    return resolveFromPersonRegistry(authenticationDetails, attributes);
  }

  private ResolvedPerson resolveFromPersonRegistry(
      AuthenticationDetails authenticationDetails, Map<String, String> attributes) {
    var startFetchingPerson = Instant.now();
    var nin = authenticationDetails.getNin().orElseThrow();
    var person =
        personRegistry.fetchPersonByNin(nin).or(() -> createPerson(attributes, nin)).orElseThrow();
    var registryAnswered = Instant.now();
    logIfDebug("Got person details from registry in {} ms.", startFetchingPerson);

    var customers = fetchCustomersWithActiveAffiliations(person);
    var users = createUsers(person, customers, authenticationDetails);
    writeLinkAndPersonRowsOrLog(person, customers, authenticationDetails, registryAnswered);
    return new ResolvedPerson(person, customers, users, authenticationDetails);
  }

  /**
   * Empty when the user has no link. A link without its person row is a data problem: a refresh
   * that carries the number repairs it through the registry, one without it gets its own error.
   */
  private Optional<ResolvedPerson> resolveFromLink(AuthenticationDetails authenticationDetails) {
    var link = identityService.fetchLink(authenticationDetails.getUsername());
    if (link.isEmpty()) {
      return Optional.empty();
    }
    var linkedPerson = identityService.fetchLinkedPerson(link.get().personId());
    if (linkedPerson.isPresent()) {
      return Optional.of(
          resolveFromLinkedPerson(authenticationDetails, link.get(), linkedPerson.get()));
    }
    if (authenticationDetails.getNin().isEmpty()) {
      throw new IdentityServiceMissingLinkException();
    }
    LOGGER.warn(
        LINK_WITHOUT_PERSON_MESSAGE, authenticationDetails.getUsername(), link.get().personId());
    return Optional.empty();
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
   * A remembered customer without a user entry gives no access. The next login that reaches the
   * person registry recreates the entry.
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
    return attempt(
            () ->
                identityService.getUserByPersonCristinIdAndCustomerCristinId(
                    person.getId(), customer.getCristinId()))
        .toOptional(
            fail ->
                LOGGER.warn(
                    NO_USER_ENTRY_MESSAGE, person.getId(), customer.getId(), fail.getException()));
  }

  /**
   * Remembers what the person registry answered, so later token generations can resolve the Cognito
   * user without the registry. Written on every login that reached the registry.
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private void writeLinkAndPersonRowsOrLog(
      Person person,
      Set<CustomerDto> customers,
      AuthenticationDetails authenticationDetails,
      Instant registryAnswered) {
    try {
      writeLinkAndPersonRows(person, customers, authenticationDetails, registryAnswered);
    } catch (RuntimeException e) {
      // The rows are a cache of what the registry answered. The login must not fail because the
      // cache could not be written; the next login through the registry writes it again.
      LOGGER.warn(COULD_NOT_WRITE_ROWS_MESSAGE, person.getId(), e);
    }
  }

  private void writeLinkAndPersonRows(
      Person person,
      Set<CustomerDto> customers,
      AuthenticationDetails authenticationDetails,
      Instant registryAnswered) {
    var customerIds = customers.stream().map(CustomerDto::getId).collect(Collectors.toSet());
    identityService.saveLinkedPerson(
        new LinkedPerson(
            person.getId(),
            person.getFirstname(),
            person.getSurname(),
            customerIds,
            registryAnswered));
    identityService.saveLink(
        new IdentityLink(
            authenticationDetails.getUsername(),
            person.getId(),
            authenticationDetails.getFeideIdentifier(),
            authenticationDetails.getFeideDomain(),
            registryAnswered));
  }

  private Optional<Person> createPerson(
      Map<String, String> attributes, NationalIdentityNumber nin) {
    var lastName = extractLastName(attributes);
    var firstName = extractFirstName(attributes);
    return personRegistry.createPerson(nin, firstName, lastName);
  }

  private String extractFirstName(Map<String, String> attributes) {
    return PersonName.fromFullName(attributes.get(CognitoClaims.NAME_CLAIM))
        .map(PersonName::givenName)
        .or(() -> PersonName.decodeProviderValue(attributes.get(CognitoClaims.FIRST_NAME_CLAIM)))
        .map(String::trim)
        .filter(name -> !name.isBlank())
        .orElseThrow(
            () ->
                getIllegalStateException(
                    "Could not extract first name from full name: %s", attributes));
  }

  private static IllegalStateException getIllegalStateException(
      String message, Map<String, String> attributes) {
    LOGGER.error(COULD_NOT_EXTRACT_NAME_MESSAGE, attributes.keySet());
    return new IllegalStateException(
        message.formatted(attributes.getOrDefault(CognitoClaims.NAME_CLAIM, null)));
  }

  @JacocoGenerated
  private String extractLastName(Map<String, String> attributes) {
    return PersonName.fromFullName(attributes.get(CognitoClaims.NAME_CLAIM))
        .map(PersonName::familyName)
        .or(() -> PersonName.decodeProviderValue(attributes.get(CognitoClaims.LAST_NAME_CLAIM)))
        .map(String::trim)
        .filter(name -> !name.isBlank())
        .orElseThrow(
            () ->
                getIllegalStateException(
                    "Could not extract last name from full name: %s", attributes));
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
              .filter(
                  customer ->
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

  private List<UserDto> createUsers(
      Person person, Set<CustomerDto> customers, AuthenticationDetails authenticationDetails) {
    var userCreationContext =
        new UserCreationContext(person, customers, authenticationDetails.getFeideIdentifier());

    var users = userCreator.createUsers(userCreationContext);

    if (users.isEmpty()) {
      LOGGER.warn("No user entries could be created for person {}", person.getId());
    }

    return users;
  }

  private static AuthenticationDetails extractAuthenticationDetails(
      CognitoUserPoolPreTokenGenerationEventV2 input) {
    var attributes = input.getRequest().getUserAttributes();
    return new AuthenticationDetails(
        extractNin(attributes).orElse(null),
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

  private Set<CustomerDto> fetchCustomersWithActiveAffiliations(final Person person) {
    return ActiveCustomers.forPerson(person, customerService);
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
