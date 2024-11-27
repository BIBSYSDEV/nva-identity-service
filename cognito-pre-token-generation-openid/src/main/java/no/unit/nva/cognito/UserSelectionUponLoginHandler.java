package no.unit.nva.cognito;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC;
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
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.database.IdentityService.defaultIdentityService;
import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import static nva.commons.apigateway.AccessRight.ACT_AS;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.ClaimsOverrideDetails;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.GroupConfiguration;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Response;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
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
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

@SuppressWarnings({"PMD.GodClass"})
public class UserSelectionUponLoginHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    public static final Environment ENVIRONMENT = new Environment();
    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));
    public static final String NIN_FOR_FEIDE_USERS = "custom:feideIdNin";
    public static final String NIN_FOR_NON_FEIDE_USERS = "custom:nin";
    public static final String FEIDE_ID = "custom:feideId";
    public static final String ORG_FEIDE_DOMAIN = "custom:orgFeideDomain";
    public static final String USER_NOT_ALLOWED_TO_IMPERSONATE = "User not allowed to impersonate";
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSelectionUponLoginHandler.class);
    private static final String CUSTOMER_IS_INACTIVE_ERROR_MESSAGE
        = "Customer is inactive {} when logging in as {} with the following affiliations: {}";
    private static final String FAILED_TO_RETRIEVE_CUSTOMER_FOR_ACTIVE_AFFILIATION
        = "Failed to retrieve customer for active affiliation %s when logging in as %s with the following "
          + "affiliations: %s";
    private final CustomerService customerService;
    private final CognitoIdentityProviderClient cognitoClient;
    private final UserEntriesCreatorForPerson userCreator;
    private final PersonRegistry personRegistry;

    @JacocoGenerated
    public UserSelectionUponLoginHandler() {
        this.cognitoClient = defaultCognitoClient();
        this.customerService = defaultCustomerService(DEFAULT_DYNAMO_CLIENT);
        this.userCreator = new UserEntriesCreatorForPerson(defaultIdentityService(DEFAULT_DYNAMO_CLIENT));
        this.personRegistry = CristinPersonRegistry.defaultPersonRegistry();
    }

    public UserSelectionUponLoginHandler(CognitoIdentityProviderClient cognitoClient,
                                         CustomerService customerService,
                                         IdentityService identityService,
                                         PersonRegistry personRegistry) {

        this.cognitoClient = cognitoClient;
        this.customerService = customerService;
        this.personRegistry = personRegistry;
        this.userCreator = new UserEntriesCreatorForPerson(identityService);
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

    @JacocoGenerated
    private static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
                   .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                   .httpClient(UrlConnectionHttpClient.create())
                   .region(AWS_REGION)
                   .build();
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {
        try {
            return processInput(input);
        } catch (Exception e) {
            LOGGER.error("Failed to process input due to", e);
            throw e;
        }
    }

    private CognitoUserPoolPreTokenGenerationEvent processInput(CognitoUserPoolPreTokenGenerationEvent input) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entering request handler...");
        }

        final var start = Instant.now();

        final var authenticationDetails = extractAuthenticationDetails(input);

        var startFetchingPerson = Instant.now();

        var impersonating = input.getRequest().getUserAttributes().get(IMPERSONATING_CLAIM);
        var nin = getCurrentNin(impersonating, authenticationDetails);

        var requestedPerson = personRegistry.fetchPersonByNin(nin);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Got person details from registry in {} ms.",
                         Instant.now().toEpochMilli() - startFetchingPerson.toEpochMilli());
        }

        List<UserAccessRightForCustomer> accessRights = emptyList();
        List<AttributeType> userAttributes = emptyList();

        if (requestedPerson.isPresent()) {
            var impersonatedBy = getImpersonatedBy(impersonating, authenticationDetails);
            var customersForPerson = fetchCustomersWithActiveAffiliations(requestedPerson.get());
            var usersForPerson = createUsers(requestedPerson.get(), customersForPerson, authenticationDetails);
            var currentCustomer = returnCurrentCustomerIfUnambiguous(authenticationDetails.getFeideDomain(), customersForPerson);

            var allowedCustomersString = createAllowedCustomersString(customersForPerson, authenticationDetails.getFeideDomain());

            if (currentCustomer.isPresent()) {
                accessRights = createAccessRightForCustomer(usersForPerson, customersForPerson, currentCustomer.get());
                userAttributes = createAttributesWithCustomer(requestedPerson.get(), accessRights, usersForPerson,
                                                              currentCustomer.get(),
                                                   allowedCustomersString, impersonatedBy);
            } else {
                userAttributes = createAttributesWithoutCustomer(requestedPerson.get(), accessRights,
                                                                 allowedCustomersString,
                                                   impersonatedBy);
            }

            updateCognitoUserAttributes(authenticationDetails, userAttributes);
        }

        injectAccessRightsToEventResponse(input, createAccessRightsWithCustomer(accessRights), userAttributes);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Leaving request handler having spent {} ms.",
                         Instant.now().toEpochMilli() - start.toEpochMilli());
        }

        return input;
    }

    private List<AttributeType> createAttributesWithoutCustomer(Person requestedPerson,
                                                  List<UserAccessRightForCustomer> accessRights,
                                                  String allowedCustomersString, String impersonatedBy) {
        List<AttributeType> userAttributes = createUserAttributes(requestedPerson,
                                              createAccessRightsWithoutCustomer(accessRights),
                                              emptySet(),
                                              allowedCustomersString,
                                              impersonatedBy);
        userAttributes.addAll(createEmptyCustomerSelection());
        return userAttributes;
    }

    private List<AttributeType> createAttributesWithCustomer(Person requestedPerson,
                                                  List<UserAccessRightForCustomer> accessRights,
                                                  List<UserDto> usersForPerson, CustomerDto currentCustomer,
                                                  String allowedCustomersString, String impersonatedBy) {
        List<AttributeType> userAttributes = createUserAttributes(requestedPerson,
                                              createAccessRightsWithoutCustomer(accessRights),
                                              rolesForCustomer(usersForPerson, currentCustomer),
                                              allowedCustomersString,
                                              impersonatedBy);
        userAttributes.addAll(
            customerSelectionClaims(currentCustomer, getCurrentUser(usersForPerson,
                                                                          currentCustomer.getId())));
        return userAttributes;
    }

    private NationalIdentityNumber getCurrentNin(String impersonating,
                                                 AuthenticationDetails authenticationDetails) {

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
                                                                                             customerForImpersonators))
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

    private List<UserDto> createUsers(Person person,
                                      Set<CustomerDto> customers,
                                      AuthenticationDetails authenticationDetails) {
        var userCreationContext = new UserCreationContext(person,
                                                          customers,
                                                          authenticationDetails.getFeideIdentifier());

        return userCreator.createUsers(userCreationContext);
    }

    private AuthenticationDetails extractAuthenticationDetails(CognitoUserPoolPreTokenGenerationEvent input) {
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

    private UserDto getCurrentUser(Collection<UserDto> users, URI currentCustomerId) {
        return users.stream()
                   .filter(user -> user.getInstitution().equals(currentCustomerId))
                   .collect(SingletonCollector.collect());
    }

    private Optional<CustomerDto> returnCurrentCustomerIfUnambiguous(
        String feideDomain,
        Set<CustomerDto> customers) {
        // Return the current customer if they are defined by Feide login, or if the person is affiliated with
        // exactly one customer.
        return customers.stream()
                   .filter(customer -> selectFeideOrgIfApplicable(customer, feideDomain))
                   .collect(SingletonCollector.tryCollect())
                   .toOptional();
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

    private Set<RoleName> rolesForCustomer(List<UserDto> usersForPerson, CustomerDto customer) {
        return usersForPerson.stream()
                   .filter(user -> user.getInstitution().equals(customer.getId()))
                   .map(UserDto::getRoles)
                   .flatMap(Collection::stream)
                   .map(RoleDto::getRoleName)
                   .collect(Collectors.toSet());
    }

    private void updateCognitoUserAttributes(
        AuthenticationDetails authenticationDetails,
        Collection<AttributeType> userAttributes) {

        var updateUserAttributesRequest = AdminUpdateUserAttributesRequest.builder()
                                              .userPoolId(authenticationDetails.getUserPoolId())
                                              .username(authenticationDetails.getUsername())
                                              .userAttributes(userAttributes)
                                              .build();

        cognitoClient.adminUpdateUserAttributes(updateUserAttributesRequest);
    }

    private List<AttributeType> createUserAttributes(
        Person person,
        Collection<String> accessRights,
        Collection<RoleName> roles,
        String allowedCustomersString,
        String impersonatedBy) {

        var claims = new ArrayList<AttributeType>();
        claims.add(createAttribute(FIRST_NAME_CLAIM, person.getFirstname()));
        claims.add(createAttribute(LAST_NAME_CLAIM, person.getSurname()));
        claims.add(createAttribute(ACCESS_RIGHTS_CLAIM, String.join(ELEMENTS_DELIMITER, accessRights)));
        claims.add(createAttribute(ROLES_CLAIM,
                                   String.join(ELEMENTS_DELIMITER, roles.stream().map(RoleName::getValue).toList())));
        claims.add(createAttribute(ALLOWED_CUSTOMERS_CLAIM, allowedCustomersString));
        claims.add(createAttribute(PERSON_CRISTIN_ID_CLAIM, person.getId().toString()));
        claims.add(createAttribute(IMPERSONATED_BY_CLAIM, isNull(impersonatedBy) ? "" : impersonatedBy));
        return claims;
    }

    private List<AttributeType> createEmptyCustomerSelection() {
        return generateCustomerSelectionClaims(EMPTY_CLAIM,
                                               EMPTY_CLAIM,
                                               EMPTY_CLAIM,
                                               EMPTY_CLAIM);
    }

    private List<AttributeType> customerSelectionClaims(CustomerDto currentCustomer, UserDto currentUser) {
        return generateCustomerSelectionClaims(currentCustomer.getId().toString(),
                                               currentCustomer.getCristinId().toString(),
                                               currentUser.getUsername(),
                                               currentUser.getAffiliation().toString());
    }

    private List<AttributeType> generateCustomerSelectionClaims(String customerId,
                                                                String topOrgCristinId,
                                                                String username,
                                                                String personAffiliation) {

        var currentCustomerClaim = createAttribute(CURRENT_CUSTOMER_CLAIM, customerId);
        var currentTopLevelOrgClaim = createAttribute(TOP_ORG_CRISTIN_ID, topOrgCristinId);
        var usernameClaim = createAttribute(NVA_USERNAME_CLAIM, username);
        var personAffiliationClaim = createAttribute(PERSON_AFFILIATION_CLAIM, personAffiliation);

        return List.of(currentCustomerClaim, currentTopLevelOrgClaim, usernameClaim, personAffiliationClaim);
    }

    private String createAllowedCustomersString(Collection<CustomerDto> allowedCustomers, String feideDomain) {
        var result = allowedCustomers
                         .stream()
                         .filter(isNotFeideRequestOrIsFeideRequestForCustomer(feideDomain))
                         .map(CustomerDto::getId)
                         .map(URI::toString)
                         .collect(Collectors.joining(ELEMENTS_DELIMITER));
        return StringUtils.isNotBlank(result)
                   ? result
                   : EMPTY_CLAIM;
    }

    private Predicate<CustomerDto> isNotFeideRequestOrIsFeideRequestForCustomer(String feideDomain) {
        return customer -> isNull(feideDomain)
                           || nonNull(customer.getFeideOrganizationDomain())
                              && customer.getFeideOrganizationDomain().equals(feideDomain);
    }

    private AttributeType createAttribute(String name, String value) {
        return AttributeType.builder().name(name).value(value).build();
    }

    private void injectAccessRightsToEventResponse(CognitoUserPoolPreTokenGenerationEvent input,
                                                   List<String> accessRights, List<AttributeType> userAttributes) {
        input.setResponse(Response.builder()
                              .withClaimsOverrideDetails(buildOverrideClaims(accessRights, userAttributes))
                              .build());
        LOGGER.info("Input version: {}", input.getVersion());
        input.setVersion("2");
    }

    private List<UserAccessRightForCustomer> createAccessRightForCustomer(List<UserDto> personUsers,
                                                                          Set<CustomerDto> customers,
                                                                          CustomerDto currentCustomer) {
        return personUsers.stream()
                   .map(user -> UserAccessRightForCustomer.fromUser(user, customers))
                   .flatMap(Collection::stream)
                   .filter(ac -> ac.getCustomer().equals(currentCustomer))
                   .toList();
    }

    private List<String> createAccessRightsWithCustomer(List<UserAccessRightForCustomer> accessRights) {
        return accessRights
                   .stream()
                   .map(UserAccessRightForCustomer::toString)
                   .collect(Collectors.toList());
    }

    private List<String> createAccessRightsWithoutCustomer(List<UserAccessRightForCustomer> accessRights) {
        return accessRights
                   .stream()
                   .map(UserAccessRightForCustomer::getAccessRight)
                   .map(AccessRight::toPersistedString)
                   .collect(Collectors.toList());
    }

    private ClaimsOverrideDetails buildOverrideClaims(List<String> groupsToOverride, List<AttributeType> userAttributes) {
        var groups = GroupConfiguration.builder()
                         .withGroupsToOverride(groupsToOverride.toArray(String[]::new))
                         .build();
        return ClaimsOverrideDetails.builder()
                   .withGroupOverrideDetails(groups)
                   .withClaimsToSuppress(CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC)
                    .withClaimsToAddOrOverride(userAttributes.stream()
                        .peek(attribute -> LOGGER.info("Adding attribute: {} = {}", attribute.name(),
                                                        attribute.value()))//remove after testing
                        .collect(Collectors.toMap(AttributeType::name, AttributeType::value)))
                   .build();
    }
}
