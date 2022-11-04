package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ELEMENTS_DELIMITER;
import static no.unit.nva.cognito.CognitoClaims.EMPTY_CLAIM_VALUE;
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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
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
    public static final String NIN_FON_NON_FEIDE_USERS = "custom:nin";
    public static final String FEIDE_ID = "custom:feideId";
    public static final String ORG_FEIDE_DOMAIN = "custom:orgFeideDomain";
    public static final String PERSON_REGISTRY_HOST = "PERSON_REGISTRY_HOST";
    private final CustomerService customerService;
    private final CognitoIdentityProviderClient cognitoClient;
    private final UserEntriesCreatorForPerson userCreator;

    @JacocoGenerated
    public UserSelectionUponLoginHandler() {
        this.cognitoClient = defaultCognitoClient();
        this.customerService = defaultCustomerService(DEFAULT_DYNAMO_CLIENT);
        this.userCreator = new UserEntriesCreatorForPerson(customerService, CristinClient.defaultClient(),
                                                           defaultIdentityService(DEFAULT_DYNAMO_CLIENT));
    }

    public UserSelectionUponLoginHandler(
        Environment environment,
        CognitoIdentityProviderClient cognitoClient,
        AuthorizedBackendClient httpClient,
        CustomerService customerService,
        IdentityService identityService) {
        var personRegistryHost = URI.create(environment.readEnv(PERSON_REGISTRY_HOST));
        this.cognitoClient = cognitoClient;
        this.customerService = customerService;
        var cristinClient = new CristinClient(personRegistryHost, httpClient);
        this.userCreator = new UserEntriesCreatorForPerson(customerService, cristinClient, identityService);
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {
        var nin = extractNin(input.getRequest().getUserAttributes());
        var orgFeideDomain = extractOrgFeideDomain(input.getRequest().getUserAttributes());
        var personFeideIdentifier = extractFeideIdentifier(input.getRequest().getUserAttributes());

        var authenticationInfo = collectAuthenticationInformation(
            nin,
            orgFeideDomain,
            personFeideIdentifier);
        final var usersForPerson = userCreator.createUsers(authenticationInfo);

        final var roles = rolesPerCustomer(usersForPerson);
        authenticationInfo.updateCurrentCustomer();
        authenticationInfo.updateCurrentUser(usersForPerson);

        final var accessRights = createAccessRightsPerCustomer(usersForPerson);
        updateCognitoUserAttributes(input, authenticationInfo, accessRights, roles);
        injectAccessRightsToEventResponse(input, accessRights);
        return input;
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

    private AuthenticationInformation collectAuthenticationInformation(String nin,
                                                                       String orgFeideDomain,
                                                                       String personFeideIdentifier) {
        return attempt(() -> new NationalIdentityNumber(nin))
                   .map(idNumber -> userCreator.collectPersonInformation(idNumber, personFeideIdentifier,
                                                                         orgFeideDomain))
                   .map(AuthenticationInformation::new)
                   .orElseThrow();
    }

    private Collection<String> rolesPerCustomer(List<UserDto> usersForPerson) {
        return usersForPerson.stream()
                   .flatMap(UserDto::generateRoleClaims)
                   .collect(Collectors.toSet());
    }

    private void updateCognitoUserAttributes(
        CognitoUserPoolPreTokenGenerationEvent input,
        AuthenticationInformation authenticationInfo,
        Collection<String> accessRights,
        Collection<String> roles) {

        cognitoClient.adminUpdateUserAttributes(createUpdateUserAttributesRequest(input,
                                                                                  authenticationInfo,
                                                                                  accessRights,
                                                                                  roles));
    }

    private AdminUpdateUserAttributesRequest createUpdateUserAttributesRequest(
        CognitoUserPoolPreTokenGenerationEvent input,
        AuthenticationInformation authenticationInfo,
        Collection<String> accessRights,
        Collection<String> roles) {

        Collection<AttributeType> userAttributes = updatedPersonAttributes(authenticationInfo, accessRights, roles);

        LOGGER.info("Updating user attributes: {}", userAttributes);

        return AdminUpdateUserAttributesRequest.builder()
                   .userPoolId(input.getUserPoolId())
                   .username(input.getUserName())
                   .userAttributes(userAttributes)
                   .build();
    }

    private Collection<AttributeType> updatedPersonAttributes(AuthenticationInformation authenticationInfo,
                                                              Collection<String> accessRights,
                                                              Collection<String> roles) {

        var allowedCustomersString = createAllowedCustomersString(authenticationInfo.getActiveCustomers(),
                                                                  authenticationInfo.getOrgFeideDomain());

        if (authenticationInfo.personExistsInPersonRegistry()) {
            return addClaimsForPeopleRegisteredInPersonRegistry(authenticationInfo,
                                                                accessRights,
                                                                roles,
                                                                allowedCustomersString);
        }
        return Collections.emptyList();
    }

    private List<AttributeType> addClaimsForPeopleRegisteredInPersonRegistry(
        AuthenticationInformation authenticationInfo,
        Collection<String> accessRights,
        Collection<String> roles,
        String allowedCustomersString) {

        var claims = new ArrayList<AttributeType>();
        claims.add(createAttribute("custom:firstName", authenticationInfo.extractFirstName()));
        claims.add(createAttribute("custom:lastName", authenticationInfo.extractLastName()));
        claims.add(createAttribute(ACCESS_RIGHTS_CLAIM, String.join(ELEMENTS_DELIMITER, accessRights)));
        claims.add(createAttribute(ROLES_CLAIM, String.join(ELEMENTS_DELIMITER, roles)));
        claims.add(createAttribute(ALLOWED_CUSTOMER_CLAIM, allowedCustomersString));
        claims.add(createAttribute(PERSON_CRISTIN_ID_CLAIM, authenticationInfo.getCristinPersonId().toString()));
        addCustomerSelectionClaimsWhenUserHasOnePossibleLoginOrLoggedInWithFeide(authenticationInfo, claims);
        return claims;
    }

    private void addCustomerSelectionClaimsWhenUserHasOnePossibleLoginOrLoggedInWithFeide(
        AuthenticationInformation authenticationInfo,
        List<AttributeType> claims) {

        authenticationInfo.getCurrentCustomerId()
            .ifPresentOrElse(generateCustomerSelectionClaimsFromAuthentication(authenticationInfo, claims),
                             clearCustomerSelectionClaimsWhenCustomerIsAmbiguous(claims));
    }

    private Runnable clearCustomerSelectionClaimsWhenCustomerIsAmbiguous(List<AttributeType> claims) {
        return () -> claims.addAll(emptyCustomerSelectionClaims());
    }

    private Consumer<String> generateCustomerSelectionClaimsFromAuthentication(
        AuthenticationInformation authenticationInfo,
        List<AttributeType> claims) {
        
        return customerId -> claims.addAll(customerSelectionClaims(authenticationInfo, customerId));
    }

    private List<AttributeType> emptyCustomerSelectionClaims() {
        return generateCustomerSelectionClaims(EMPTY_CLAIM_VALUE,
                                               EMPTY_CLAIM_VALUE,
                                               EMPTY_CLAIM_VALUE,
                                               EMPTY_CLAIM_VALUE);
    }

    private List<AttributeType> customerSelectionClaims(AuthenticationInformation authenticationInfo,
                                                        String customerId) {
        return generateCustomerSelectionClaims(customerId,
                                               authenticationInfo.getCurrentCustomer().getCristinId().toString(),
                                               authenticationInfo.getCurrentUser().getUsername(),
                                               authenticationInfo.getCurrentUser().getAffiliation().toString());
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
                   : EMPTY_CLAIM_VALUE;
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
