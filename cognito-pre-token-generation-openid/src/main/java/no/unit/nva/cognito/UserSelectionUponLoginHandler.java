package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ELEMENTS_DELIMITER;
import static no.unit.nva.cognito.CognitoClaims.EMPTY_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.FIRST_NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.LAST_NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ROLES_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.database.IdentityService.defaultIdentityService;
import static no.unit.useraccessservice.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.UserCreationContext;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

public class UserSelectionUponLoginHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSelectionUponLoginHandler.class);
    public static final Environment ENVIRONMENT = new Environment();
    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));
    public static final String NIN_FOR_FEIDE_USERS = "custom:feideIdNin";
    public static final String NIN_FOR_NON_FEIDE_USERS = "custom:nin";
    public static final String FEIDE_ID = "custom:feideId";
    public static final String ORG_FEIDE_DOMAIN = "custom:orgFeideDomain";
    public static final String COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR = "Could not find user for customer: ";

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

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entering request handler...");
        }

        final var start = Instant.now();

        final var authenticationDetails = extractAuthenticationDetails(input);

        var startFetchingPerson = Instant.now();

        var impersonating = input.getRequest().getUserAttributes().get("custom:impersonating");
        LOGGER.info("Impersonating: {}", impersonating);
        var nin = isNull(impersonating)
                      ? authenticationDetails.getNin()
                      : new NationalIdentityNumber(impersonating);
        var optionalPerson = personRegistry.fetchPersonByNin(nin);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Got person details from registry in {} ms.",
                         Instant.now().toEpochMilli() - startFetchingPerson.toEpochMilli());
        }

        if (optionalPerson.isPresent()) {
            var accessRights = createUsersAndUpdateCognitoBasedOnPersonRegistry(optionalPerson.get(),
                                                                                authenticationDetails);
            Map<String, String> overrideClaims = isNull(impersonating)
                                     ? Map.of()
                                     : Map.of(
                                         "custom:impersonatedBy", authenticationDetails.getUsername()
                                     );
            injectAccessRightsToEventResponse(input, accessRights, overrideClaims);
        } else {
            injectAccessRightsToEventResponse(input, Collections.emptyList(), Map.of());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Leaving request handler having spent {} ms.",
                         Instant.now().toEpochMilli() - start.toEpochMilli());
        }

        LOGGER.info(input.getResponse().toString());
        return input;
    }

    private List<String> createUsersAndUpdateCognitoBasedOnPersonRegistry(Person person,
                                                                          AuthenticationDetails authenticationDetails) {
        var start = Instant.now();
        var customersForPerson = fetchCustomersWithActiveAffiliations(person.getAffiliations());
        var usersForPerson = createUsers(person, customersForPerson, authenticationDetails);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created users for customer with active affiliations in {} ms.",
                         Instant.now().toEpochMilli() - start.toEpochMilli());
        }

        start = Instant.now();
        var accessRights = updateUserAttributesInCognito(person, customersForPerson, usersForPerson,
                                                         authenticationDetails);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Updated user attributes in Cognito in {} ms.",
                         Instant.now().toEpochMilli() - start.toEpochMilli());
        }

        return accessRights;
    }

    private List<String> updateUserAttributesInCognito(Person person,
                                                       Set<CustomerDto> customers,
                                                       List<UserDto> users,
                                                       AuthenticationDetails authenticationDetails) {

        var rolesPerCustomerForPerson = rolesPerCustomer(users);
        var currentCustomer
            = returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer(
            authenticationDetails.getFeideDomain(), customers);
        var currentUser = nonNull(currentCustomer)
                              ? getCurrentUser(currentCustomer, users)
                              : null;

        var accessRights = createAccessRightsPerCustomer(users, customers);

        updateCognitoUserAttributes(authenticationDetails,
                                    person,
                                    currentCustomer,
                                    currentUser,
                                    customers,
                                    accessRights,
                                    rolesPerCustomerForPerson);

        return accessRights;
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
        var nin = extractNin(input.getRequest().getUserAttributes());
        var feideDomain = extractOrgFeideDomain(input.getRequest().getUserAttributes());
        var feideIdentifier = extractFeideIdentifier(input.getRequest().getUserAttributes());
        var userPoolId = input.getUserPoolId();
        var username = input.getUserName();

        return new AuthenticationDetails(nin, feideIdentifier, feideDomain, userPoolId, username);
    }

    private UserDto getCurrentUser(CustomerDto currentCustomer, Collection<UserDto> users) {
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

    private Set<CustomerDto> fetchCustomersWithActiveAffiliations(List<Affiliation> affiliations) {
        return affiliations.stream()
                   .map(Affiliation::getInstitutionId)
                   .map(attempt(customerService::getCustomerByCristinId))
                   .flatMap(Try::stream)
                   .collect(Collectors.toSet());
    }

    private static NationalIdentityNumber extractNin(Map<String, String> userAttributes) {
        return new NationalIdentityNumber(
            Optional.ofNullable(userAttributes.get(NIN_FOR_FEIDE_USERS))
                .or(() -> Optional.ofNullable(userAttributes.get(NIN_FOR_NON_FEIDE_USERS)))
                .orElseThrow());
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

    private Set<String> rolesPerCustomer(List<UserDto> usersForPerson) {
        return usersForPerson.stream()
                   .flatMap(UserDto::generateRoleClaims)
                   .collect(Collectors.toSet());
    }

    private void updateCognitoUserAttributes(
        AuthenticationDetails authenticationDetails,
        Person person,
        CustomerDto currentCustomer,
        UserDto currentUser,
        Set<CustomerDto> customers,
        Collection<String> accessRights,
        Collection<String> roles) {

        final var updateUserAttributesRequest = createUpdateUserAttributesRequest(
            authenticationDetails,
            person,
            currentCustomer,
            currentUser,
            customers,
            accessRights,
            roles);

        cognitoClient.adminUpdateUserAttributes(updateUserAttributesRequest);
    }

    private AdminUpdateUserAttributesRequest createUpdateUserAttributesRequest(
        AuthenticationDetails authenticationDetails,
        Person person,
        CustomerDto currentCustomer,
        UserDto currentUser,
        Set<CustomerDto> customers,
        Collection<String> accessRights,
        Collection<String> roles) {

        Collection<AttributeType> userAttributes = updatedPersonAttributes(person,
                                                                           authenticationDetails.getFeideDomain(),
                                                                           currentCustomer,
                                                                           currentUser,
                                                                           customers,
                                                                           accessRights,
                                                                           roles);

        return AdminUpdateUserAttributesRequest.builder()
                   .userPoolId(authenticationDetails.getUserPoolId())
                   .username(authenticationDetails.getUsername())
                   .userAttributes(userAttributes)
                   .build();
    }

    private Collection<AttributeType> updatedPersonAttributes(Person person,
                                                              String feideDomain,
                                                              CustomerDto currentCustomer,
                                                              UserDto currentUser,
                                                              Set<CustomerDto> customers,
                                                              Collection<String> accessRights,
                                                              Collection<String> roles) {

        var allowedCustomersString = createAllowedCustomersString(customers, feideDomain);

        return addClaimsForPeopleRegisteredInPersonRegistry(person,
                                                            currentCustomer,
                                                            currentUser,
                                                            accessRights,
                                                            roles,
                                                            allowedCustomersString);
    }

    private List<AttributeType> addClaimsForPeopleRegisteredInPersonRegistry(
        Person person,
        CustomerDto currentCustomer,
        UserDto currentUser,
        Collection<String> accessRights,
        Collection<String> roles,
        String allowedCustomersString) {

        var claims = new ArrayList<AttributeType>();
        claims.add(createAttribute(FIRST_NAME_CLAIM, person.getFirstname()));
        claims.add(createAttribute(LAST_NAME_CLAIM, person.getSurname()));
        claims.add(createAttribute(ACCESS_RIGHTS_CLAIM, String.join(ELEMENTS_DELIMITER, accessRights)));
        claims.add(createAttribute(ROLES_CLAIM, String.join(ELEMENTS_DELIMITER, roles)));
        claims.add(createAttribute(ALLOWED_CUSTOMERS_CLAIM, allowedCustomersString));
        claims.add(createAttribute(PERSON_CRISTIN_ID_CLAIM, person.getId().toString()));
        addCustomerSelectionClaimsWhenUserHasOnePossibleLoginOrLoggedInWithFeide(currentCustomer, currentUser, claims);
        return claims;
    }

    private void addCustomerSelectionClaimsWhenUserHasOnePossibleLoginOrLoggedInWithFeide(
        CustomerDto currentCustomer,
        UserDto currentUser,
        List<AttributeType> claims) {

        if (currentCustomer != null) {
            generateCustomerSelectionClaimsFromAuthentication(currentCustomer, currentUser, claims);
        } else {
            clearCustomerSelectionClaimsWhenCustomerIsAmbiguous(claims);
        }
    }

    private void clearCustomerSelectionClaimsWhenCustomerIsAmbiguous(List<AttributeType> claims) {
        claims.addAll(overwriteCustomerSelectionClaimsWithNullString());
    }

    private void generateCustomerSelectionClaimsFromAuthentication(
        CustomerDto currentCustomer,
        UserDto currentUser,
        List<AttributeType> claims) {

        claims.addAll(customerSelectionClaims(currentCustomer, currentUser));
    }

    private List<AttributeType> overwriteCustomerSelectionClaimsWithNullString() {
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
                                                   List<String> accessRights,
                                                   Map<String, String> claimsToOverride) {
        input.setResponse(Response.builder()
                              .withClaimsOverrideDetails(buildOverrideClaims(claimsToOverride, accessRights))
                              .build());
    }

    private List<String> createAccessRightsPerCustomer(List<UserDto> personUsers, Set<CustomerDto> customers) {
        return personUsers.stream()
                   .map(user -> UserAccessRightForCustomer.fromUser(user, customers))
                   .flatMap(Collection::stream)
                   .map(UserAccessRightForCustomer::toString)
                   .collect(Collectors.toList());
    }

    private ClaimsOverrideDetails buildOverrideClaims(Map<String, String> claimsToOverride, List<String> groupsToOverride) {
        var groups = GroupConfiguration.builder()
                         .withGroupsToOverride(groupsToOverride.toArray(String[]::new))
                         .build();
        return ClaimsOverrideDetails.builder()
                   .withGroupOverrideDetails(groups)
                   .withClaimsToAddOrOverride(claimsToOverride)
                   .withClaimsToSuppress(CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC)
                   .build();
    }
}
