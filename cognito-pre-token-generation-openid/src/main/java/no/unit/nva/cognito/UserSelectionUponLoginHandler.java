package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.AccessTokenGeneration;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.ClaimsAndScopeOverrideDetails;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.GroupOverrideDetails;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.IdTokenGeneration;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.Response;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2;
import java.util.Arrays;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.TermsAndConditionsService;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.UserCreationContext;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_INCLUDED_IN_ACCESS_TOKEN;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.FEIDE_ID;
import static no.unit.nva.cognito.CognitoClaims.FIRST_NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.IMPERSONATING_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.LAST_NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.cognito.CognitoClaims.NIN_FOR_NON_FEIDE_USERS;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import static no.unit.nva.database.IdentityService.defaultIdentityService;
import static nva.commons.apigateway.AccessRight.ACT_AS;
import static nva.commons.core.attempt.Try.attempt;

@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class UserSelectionUponLoginHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEventV2, CognitoUserPoolPreTokenGenerationEventV2> {

    public static final Environment ENVIRONMENT = new Environment();
    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));
    public static final String ORG_FEIDE_DOMAIN = "custom:orgFeideDomain";
    public static final String COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR = "Could not find user for customer: ";
    public static final String USER_NOT_ALLOWED_TO_IMPERSONATE = "User not allowed to impersonate";
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSelectionUponLoginHandler.class);
    private static final String CUSTOMER_IS_INACTIVE_ERROR_MESSAGE
        = "Customer is inactive {} when logging in as {} with the following affiliations: {}";
    private static final String FAILED_TO_RETRIEVE_CUSTOMER_FOR_ACTIVE_AFFILIATION
        = "Failed to retrieve customer for active affiliation %s when logging in as %s with the following "
          + "affiliations: %s";
    public static final String TRIGGER_SOURCE_REFRESH_TOKENS = "TokenGeneration_RefreshTokens";
    private final CustomerService customerService;
    private final CognitoIdentityProviderClient cognitoClient;
    private final UserEntriesCreatorForPerson userCreator;
    private final PersonRegistry personRegistry;
    private final TermsAndConditionsService termsService;

    @JacocoGenerated
    public UserSelectionUponLoginHandler() {
        this.cognitoClient = defaultCognitoClient();
        this.customerService = defaultCustomerService(DEFAULT_DYNAMO_CLIENT);
        this.userCreator = new UserEntriesCreatorForPerson(defaultIdentityService(DEFAULT_DYNAMO_CLIENT));
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
        this.personRegistry = personRegistry;
        this.userCreator = new UserEntriesCreatorForPerson(identityService);
        this.termsService = termsService;
    }

    private static NationalIdentityNumber extractNin(Map<String, String> userAttributes) {
        return
            Optional.ofNullable(userAttributes.get(NIN_FOR_FEIDE_USERS))
                .map(NationalIdentityNumber::fromString)
                .or(() -> Optional.ofNullable(userAttributes.get(NIN_FOR_NON_FEIDE_USERS))
                              .map(NationalIdentityNumber::fromString))
                .orElseThrow();
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
            return processInput(input);
        } catch (Exception e) {
            LOGGER.error("Failed to process input due to", e);
            throw e;
        }
    }

    private CognitoUserPoolPreTokenGenerationEventV2 processInput(CognitoUserPoolPreTokenGenerationEventV2 input) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entering request handler...");
        }

        final var start = Instant.now();

        LOGGER.info("DEBUG CODE (REMOVE ME): {}", attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(input)).get());

        final var authenticationDetails = extractAuthenticationDetails(input);

        var startFetchingPerson = Instant.now();

        var attributes = input.getRequest().getUserAttributes();
        var impersonating = attributes.get(IMPERSONATING_CLAIM);
        var nin = getCurrentNin(impersonating, authenticationDetails);

        var requestedPerson = personRegistry.fetchPersonByNin(nin)
                                  .or(() -> personRegistry.createPersonByNin(nin,
                                                                             attributes.getOrDefault(FIRST_NAME_CLAIM,
                                                                                                     "N/A"),
                                                                             attributes.getOrDefault(LAST_NAME_CLAIM,
                                                                                                     "N/A")));

        logIfDebug("Got person details from registry in {} ms.", startFetchingPerson);

        if (requestedPerson.isPresent()) {
            var impersonatedBy = getImpersonatedBy(impersonating, authenticationDetails);
            var person = requestedPerson.get();

            var customersForPerson = fetchCustomersWithActiveAffiliations(person);
            var currentCustomer = getCurrentCustomer(authenticationDetails, customersForPerson,
                                                     input.getTriggerSource(), attributes);

            var users = createUsers(person, customersForPerson, authenticationDetails);
            var currentTerms = termsService
                                   .getCurrentTermsAndConditions()
                                   .termsConditionsUri();
            var acceptedTerms = getAcceptedTerms(person);
            var hasAcceptedTerms = currentTerms.equals(acceptedTerms);

            var currentUser = getCurrentUser(currentCustomer, users);

            var userAccessRights = createAccessRightForCustomer(users, customersForPerson, currentCustomer,
                                                                hasAcceptedTerms);

            var accessRightsPersistedFormat = userAccessRights.stream().map(UserAccessRightForCustomer::getAccessRight)
                                                  .map(AccessRight::toPersistedString)
                                                  .toList();
            var accessRightsResponseStrings = userAccessRights.stream()
                                                  .map(UserAccessRightForCustomer::toString)
                                                  .toList();

            List<AttributeType> userAttributes = new UserAttributesBuilder()
                                                     .withPerson(person)
                                                     .withCurrentTerms(currentTerms)
                                                     .withAcceptedTerms(acceptedTerms)
                                                     .withUsers(users)
                                                     .withCustomersForPerson(customersForPerson)
                                                     .withCurrentCustomer(currentCustomer)
                                                     .withAuthenticationDetails(authenticationDetails)
                                                     .withImpersonatedBy(impersonatedBy)
                                                     .withCurrentUser(currentUser)
                                                     .withAccessRightsPersistedFormat(accessRightsPersistedFormat)
                                                     .build();

            updateCognitoUserAttributes(userAttributes,
                                        authenticationDetails.getUserPoolId(),
                                        authenticationDetails.getUsername()
            );

            input.setResponse(Response.builder()
                                  .withClaimsAndScopeOverrideDetails(
                                      buildOverrideClaims(accessRightsResponseStrings, userAttributes))
                                  .build());
        } else {
            input.setResponse(Response.builder()
                                  .withClaimsAndScopeOverrideDetails(
                                      buildOverrideClaims(Collections.emptyList(), Collections.emptyList()))
                                  .build());
        }

        logIfDebug("Leaving request handler having spent {} ms.", start);

        return input;
    }

    private CustomerDto getCurrentCustomer(AuthenticationDetails authenticationDetails,
                                           Set<CustomerDto> customersForPerson, String triggerSource,
                                           Map<String, String> userAttributes) {
        var currentCustomer = returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer(
            authenticationDetails.getFeideDomain(), customersForPerson);

        if (TRIGGER_SOURCE_REFRESH_TOKENS.equals(triggerSource)) {
            // If the user is refreshing tokens, the current customer will be set to the previously selected customer
            currentCustomer = customersForPerson.stream()
                                  .filter(customer -> customer.getId()
                                                          .equals(Optional.ofNullable(
                                                                  userAttributes.getOrDefault(CURRENT_CUSTOMER_CLAIM,
                                                                                              null))
                                                                      .map(URI::create)
                                                                      .orElse(null)))
                                  .collect(SingletonCollector.tryCollect())
                                  .orElse(fail -> null);
        }

        return currentCustomer;
    }

    private NationalIdentityNumber getCurrentNin(
        String impersonating, AuthenticationDetails authenticationDetails) {

        if (isNull(impersonating)) {
            return authenticationDetails.getNin();
        }
        var impersonator = personRegistry.fetchPersonByNin(authenticationDetails.getNin()).get();

        LOGGER.info("User {} {} impersonating: {}",
                    impersonator.getIdentifier(),
                    authenticationDetails.getUsername(),
                    impersonating);

        var customerForImpersonators = fetchCustomersWithActiveAffiliations(impersonator);
        var usersForImpersonator = createUsers(impersonator, customerForImpersonators, authenticationDetails);
        var impersonatorsAccessRights = usersForImpersonator
                                            .stream()
                                            .map(user -> UserAccessRightForCustomer.fromUser(user,
                                                                                             customerForImpersonators
                                                , true))
                                            .flatMap(Collection::stream)
                                            .map(UserAccessRightForCustomer::getAccessRight)
                                            .collect(Collectors.toSet());

        var isAllowedToImpersonate = impersonatorsAccessRights.contains(ACT_AS);

        if (!isAllowedToImpersonate) {
            LOGGER.warn(USER_NOT_ALLOWED_TO_IMPERSONATE);
            throw new IllegalStateException(USER_NOT_ALLOWED_TO_IMPERSONATE);
        }

        return NationalIdentityNumber.fromString(impersonating);
    }

    private String getImpersonatedBy(String impersonating, AuthenticationDetails authenticationDetails) {
        return isNull(impersonating) ? null : authenticationDetails.getUsername();
    }

    private URI getAcceptedTerms(Person person) {
        return Optional.ofNullable(termsService
                                       .getTermsAndConditionsByPerson(
                                           person.getId()))
                   .map(
                       TermsConditionsResponse::termsConditionsUri).orElse(null);
    }

    private List<UserDto> createUsers(Person person,
                                      Set<CustomerDto> customers,
                                      AuthenticationDetails authenticationDetails) {
        var userCreationContext = new UserCreationContext(
            person,
            customers,
            authenticationDetails.getFeideIdentifier()
        );

        return userCreator.createUsers(userCreationContext);
    }

    private AuthenticationDetails extractAuthenticationDetails(CognitoUserPoolPreTokenGenerationEventV2 input) {
        try {
            var nin = extractNin(input.getRequest().getUserAttributes());
            var feideDomain = extractOrgFeideDomain(input.getRequest().getUserAttributes());
            var feideIdentifier = extractFeideIdentifier(input.getRequest().getUserAttributes());
            var userPoolId = input.getUserPoolId();
            var username = input.getUserName();

            return new AuthenticationDetails(nin, feideIdentifier, feideDomain, userPoolId, username);
        } catch (Exception e) {
            LOGGER.error("Could not extract required data from request", e);
            LOGGER.error("User name: {}, userPoolId: {}, input request: {}", input.getUserName(), input.getUserPoolId(),
                         input.getRequest());
            throw e;
        }
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

    private CustomerDto returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer(
        String feideDomain,
        Set<CustomerDto> customers) {

        return customers.stream()
                   .filter(customer -> selectFeideOrgIfApplicable(customer, feideDomain))
                   .collect(SingletonCollector.tryCollect())
                   .orElse(fail -> null);
    }

    private boolean selectFeideOrgIfApplicable(CustomerDto customer, String feideDomain) {
        return feideDomain == null
               || feideDomain.equals(customer.getFeideOrganizationDomain());
    }

    private Set<CustomerDto> fetchCustomersWithActiveAffiliations(final Person person) {
        return person.getAffiliations().stream()
                   .map(Affiliation::getInstitutionId)
                   .map(institutionId -> getCustomerByCristinIdOrLogError(institutionId, person))
                   .flatMap(Optional::stream)
                   .filter(customer -> logInactiveInstitutions(customer, person))
                   .collect(Collectors.toSet());
    }

    private boolean logInactiveInstitutions(CustomerDto customerDto, Person person) {
        if (!customerDto.isActive()) {
            LOGGER.info(CUSTOMER_IS_INACTIVE_ERROR_MESSAGE, customerDto, person.getId(), person.getAffiliations());
        }
        return customerDto.isActive();
    }

    private Optional<CustomerDto> getCustomerByCristinIdOrLogError(URI organizationId, Person person) {
        return attempt(() -> customerService.getCustomerByCristinId(organizationId))
                   .map(Optional::of)
                   .orElse(fail -> logFailure(fail, organizationId, person));
    }

    private Optional<CustomerDto> logFailure(Failure<Optional<CustomerDto>> fail, URI organizationId, Person person) {
        var message = String.format(FAILED_TO_RETRIEVE_CUSTOMER_FOR_ACTIVE_AFFILIATION,
                                    organizationId,
                                    person.getId(),
                                    person.getAffiliations());
        LOGGER.info(message, fail.getException());
        return Optional.empty();
    }

    private void updateCognitoUserAttributes(Collection<AttributeType> userAttributes,
                                             String userPoolId, String username) {
        final var updateUserAttributesRequest =
            createUpdateUserAttributesRequest(userAttributes, userPoolId, username);
        cognitoClient.adminUpdateUserAttributes(updateUserAttributesRequest);
    }

    private AdminUpdateUserAttributesRequest createUpdateUserAttributesRequest(Collection<AttributeType> userAttributes,
                                                                               String userPoolId, String username) {

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

    private ClaimsAndScopeOverrideDetails buildOverrideClaims(List<String> groupsToOverride,
                                                              List<AttributeType> userAttributes) {
        var groups = GroupOverrideDetails.builder()
                         .withGroupsToOverride(groupsToOverride.toArray(String[]::new))
                         .build();

        return ClaimsAndScopeOverrideDetails.builder()
                   .withGroupOverrideDetails(groups)
                   .withAccessTokenGeneration(buildAccessTokenGeneration(userAttributes))
                   .withIdTokenGeneration(buildIdTokenGeneration())
                   .build();
    }

    private IdTokenGeneration buildIdTokenGeneration() {
        var excludedClaims = Stream.concat(Arrays.stream(CLAIMS_TO_BE_INCLUDED_IN_ACCESS_TOKEN),
                                           Arrays.stream(CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC))
                                 .toArray(String[]::new);
        return IdTokenGeneration.builder().withClaimsToSuppress(excludedClaims).build();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private AccessTokenGeneration buildAccessTokenGeneration(List<AttributeType> userAttributes) {
        var includedClaims = Arrays.asList(CLAIMS_TO_BE_INCLUDED_IN_ACCESS_TOKEN);
        var claims = userAttributes.stream()
                         .filter(a -> includedClaims.contains(a.name()) && nonNull(a.value()))
                         .collect(Collectors.toMap(AttributeType::name, AttributeType::value));

        return AccessTokenGeneration.builder()
                   .withClaimsToAddOrOverride(claims)
                   .build();
    }
}
