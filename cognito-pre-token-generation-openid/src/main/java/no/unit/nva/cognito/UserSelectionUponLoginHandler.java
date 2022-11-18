package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ELEMENTS_DELIMITER;
import static no.unit.nva.cognito.CognitoClaims.EMPTY_CLAIM;
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
import no.unit.nva.useraccessservice.usercreation.PersonInformation;
import no.unit.nva.useraccessservice.usercreation.PersonInformationImpl;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.PersonAffiliation;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

public class UserSelectionUponLoginHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    public static final Environment ENVIRONMENT = new Environment();

    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));
    public static final String NIN_FOR_FEIDE_USERS = "custom:feideIdNin";
    public static final String NIN_FON_NON_FEIDE_USERS = "custom:nin";
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

        var nin = extractNin(input.getRequest().getUserAttributes());
        var orgFeideDomain = extractOrgFeideDomain(input.getRequest().getUserAttributes());
        var personFeideIdentifier = extractFeideIdentifier(input.getRequest().getUserAttributes());

        var personInformation = new PersonInformationImpl(personRegistry,
                                                          nin,
                                                          personFeideIdentifier,
                                                          orgFeideDomain);
        var customers = fetchCustomersWithActiveAffiliations(personInformation.getPersonAffiliations());
        final var usersForPerson = userCreator.createUsers(personInformation, customers);

        final var roles = rolesPerCustomer(usersForPerson);
        var currentCustomer
            = returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer(orgFeideDomain,
                                                                                                 customers);
        UserDto currentUser = null;
        if (currentCustomer != null) {
            currentUser = getCurrentUser(currentCustomer, usersForPerson);
        }

        final var accessRights = createAccessRightsPerCustomer(usersForPerson);
        updateCognitoUserAttributes(input,
                                    personInformation,
                                    currentCustomer,
                                    currentUser,
                                    customers,
                                    accessRights,
                                    roles);
        injectAccessRightsToEventResponse(input, accessRights);
        return input;
    }

    private UserDto getCurrentUser(CustomerDto currentCustomer, Collection<UserDto> users) {
        if (nonNull(currentCustomer)) {
            var currentCustomerId = currentCustomer.getId();
            return attempt(() -> filterOutUser(users, currentCustomerId))
                       .orElseThrow(fail -> handleUserNotFoundError(currentCustomerId));
        } else {
            throw handleUserNotFoundError(null);
        }
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

    private Set<CustomerDto> fetchCustomersWithActiveAffiliations(List<PersonAffiliation> personAffiliations) {
        return personAffiliations.stream()
                   .map(PersonAffiliation::getInstitutionCristinId)
                   .map(attempt(customerService::getCustomerByCristinId))
                   .flatMap(Try::stream)
                   .collect(Collectors.toSet());
    }

    private static String extractNin(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(NIN_FOR_FEIDE_USERS))
                   .or(() -> Optional.ofNullable(userAttributes.get(NIN_FON_NON_FEIDE_USERS)))
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

    //    private AuthenticationInformation collectAuthenticationInformation(String nin,
    //                                                                       String orgFeideDomain,
    //                                                                       String personFeideIdentifier) {
    //        return attempt(() -> new NationalIdentityNumber(nin))
    //                   .map(idNumber -> userCreator.collectPersonInformation(idNumber, personFeideIdentifier,
    //                                                                         orgFeideDomain))
    //                   .map(AuthenticationInformation::new)
    //                   .orElseThrow();
    //    }
    //
    private Collection<String> rolesPerCustomer(List<UserDto> usersForPerson) {
        return usersForPerson.stream()
                   .flatMap(UserDto::generateRoleClaims)
                   .collect(Collectors.toSet());
    }

    private void updateCognitoUserAttributes(
        CognitoUserPoolPreTokenGenerationEvent input,
        PersonInformation personInformation,
        CustomerDto currentCustomer,
        UserDto currentUser,
        Set<CustomerDto> customers,
        Collection<String> accessRights,
        Collection<String> roles) {

        cognitoClient.adminUpdateUserAttributes(createUpdateUserAttributesRequest(input,
                                                                                  personInformation,
                                                                                  currentCustomer,
                                                                                  currentUser,
                                                                                  customers,
                                                                                  accessRights,
                                                                                  roles));
    }

    private AdminUpdateUserAttributesRequest createUpdateUserAttributesRequest(
        CognitoUserPoolPreTokenGenerationEvent input,
        PersonInformation personInformation,
        CustomerDto currentCustomer,
        UserDto currentUser,
        Set<CustomerDto> customers,
        Collection<String> accessRights,
        Collection<String> roles) {

        Collection<AttributeType> userAttributes = updatedPersonAttributes(personInformation,
                                                                           currentCustomer,
                                                                           currentUser,
                                                                           customers,
                                                                           accessRights,
                                                                           roles);

        return AdminUpdateUserAttributesRequest.builder()
                   .userPoolId(input.getUserPoolId())
                   .username(input.getUserName())
                   .userAttributes(userAttributes)
                   .build();
    }

    private Collection<AttributeType> updatedPersonAttributes(PersonInformation personInformation,
                                                              CustomerDto currentCustomer,
                                                              UserDto currentUser,
                                                              Set<CustomerDto> customers,
                                                              Collection<String> accessRights,
                                                              Collection<String> roles) {

        var allowedCustomersString = createAllowedCustomersString(customers,
                                                                  personInformation.getFeideDomain());

        if (personInformation.personIsPresentInPersonRegistry()) {
            return addClaimsForPeopleRegisteredInPersonRegistry(personInformation,
                                                                currentCustomer,
                                                                currentUser,
                                                                accessRights,
                                                                roles,
                                                                allowedCustomersString);
        }
        return Collections.emptyList();
    }

    private List<AttributeType> addClaimsForPeopleRegisteredInPersonRegistry(
        PersonInformation personInformation,
        CustomerDto currentCustomer,
        UserDto currentUser,
        Collection<String> accessRights,
        Collection<String> roles,
        String allowedCustomersString) {

        var claims = new ArrayList<AttributeType>();
        claims.add(createAttribute("custom:firstName", personInformation.getFamilyName().orElseThrow()));
        claims.add(createAttribute("custom:lastName", personInformation.getFamilyName().orElseThrow()));
        claims.add(createAttribute(ACCESS_RIGHTS_CLAIM, String.join(ELEMENTS_DELIMITER, accessRights)));
        claims.add(createAttribute(ROLES_CLAIM, String.join(ELEMENTS_DELIMITER, roles)));
        claims.add(createAttribute(ALLOWED_CUSTOMERS_CLAIM, allowedCustomersString));
        claims.add(createAttribute(PERSON_CRISTIN_ID_CLAIM,
                                   personInformation.getPersonRegistryId().orElseThrow().toString()));
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
                                                   List<String> accessRights) {
        input.setResponse(Response.builder()
                              .withClaimsOverrideDetails(buildOverrideClaims(accessRights))
                              .build());
    }

    private List<String> createAccessRightsPerCustomer(List<UserDto> personsUsers) {
        return personsUsers.stream()
                   .map(user -> UserAccessRightForCustomer.fromUser(user, customerService))
                   .flatMap(Collection::stream)
                   .map(UserAccessRightForCustomer::toString)
                   .collect(Collectors.toList());
    }

    private ClaimsOverrideDetails buildOverrideClaims(List<String> groupsToOverride) {
        var groups = GroupConfiguration.builder()
                         .withGroupsToOverride(groupsToOverride.toArray(String[]::new))
                         .build();
        return ClaimsOverrideDetails.builder()
                   .withGroupOverrideDetails(groups)
                   .withClaimsToSuppress(CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC)
                   .build();
    }
}
