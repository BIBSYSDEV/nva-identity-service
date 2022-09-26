package no.unit.nva.cognito;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.regions.Region;

public class UserSelectionUponLoginHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {
    
    public static final Environment ENVIRONMENT = new Environment();
    
    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));
    public static final String NIN_FOR_FEIDE_USERS = "custom:feideIdNin";
    public static final String NIN_FON_NON_FEIDE_USERS = "custom:nin";
    
    public static final Map<String, String> GRANT_TYPE_CLIENT_CREDENTIALS = Map.of("grant_type", "client_credentials");
    
    public static final String FEIDE_ID = "custom:feideId";
    public static final String ORG_FEIDE_DOMAIN = "custom:orgFeideDomain";
    public static final String INJECTED_ACCESS_RIGHT_TO_SINGLE_CUSTOMER_ID_IN_COGNITO_GROUPS = "USER";
    public static final int SINGLE_ITEM_LIST = 1;
    private static final int SINGLE_ITEM = 0;
    //    private final CustomerService customerService;
    //    private final CognitoIdentityProviderClient cognitoClient;
    //    private final UserEntriesCreatorForPerson userCreator;
    public static final String PERSON_REGISTRY_HOST = "PERSON_REGISTRY_HOST";
    private final Map<String, Object> buffer;
    private final AuthorizedBackendClient httpClient;
    private final Environment environment;
    
    public UserSelectionUponLoginHandler() {
        this(new Environment(), new ConcurrentHashMap<>(), null);
    }
    
    @JacocoGenerated
    public UserSelectionUponLoginHandler(
        Environment environment,
        Map<String, Object> buffer,
        AuthorizedBackendClient httpClient) {
        this.buffer = buffer;
        this.httpClient = httpClient;
        this.environment = environment;
    }
    
    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {
        var nin = extractNin(input.getRequest().getUserAttributes());
        
        var body = attempt(() -> fetchCristinInformation(nin)).orElseThrow();
        buffer.put("cristin", body);
        return input;
    }
    
    private static NationalIdentityNumber extractNin(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(NIN_FOR_FEIDE_USERS))
                   .or(() -> Optional.ofNullable(userAttributes.get(NIN_FON_NON_FEIDE_USERS)))
                   .map(NationalIdentityNumber::new)
                   .orElseThrow();
    }
    
    private CristinPersonResponse fetchCristinInformation(NationalIdentityNumber nin)
        throws IOException, InterruptedException, BadGatewayException {
        var personRegistryUri = UriWrapper.fromUri(environment.readEnv(PERSON_REGISTRY_HOST))
                                    .getUri();
        var client = new CristinClient(personRegistryUri, httpClient);
        return client.sendRequestToCristin(nin);
    }
    
    //    @SuppressWarnings("unused")
    //    public CognitoUserPoolPreTokenGenerationEvent handleRequest2(CognitoUserPoolPreTokenGenerationEvent input,
    //                                                                 Context context) {
    //        var nin = extractNin(input.getRequest().getUserAttributes());
    //        var orgFeideDomain = extractOrgFeideDomain(input.getRequest().getUserAttributes());
    //        var personFeideIdentifier = extractFeideIdentifier(input.getRequest().getUserAttributes());
    //
    //        var authenticationInfo = collectAuthenticationInformation(nin, orgFeideDomain, personFeideIdentifier);
    //        final var usersForPerson = userCreator.createUsers(authenticationInfo);
    //
    //        final var roles = rolesPerCustomer(usersForPerson);
    //        authenticationInfo.updateCurrentCustomer();
    //        authenticationInfo.updateCurrentUser(usersForPerson);
    //
    //        final var accessRights = createAccessRights(usersForPerson);
    //        updateCognitoUserAttributes(input, authenticationInfo, accessRights, roles);
    //        injectAccessRightsToEventResponse(input, accessRights);
    //        return input;
    //    }
    //
    
    //
    //    private static String extractOrgFeideDomain(Map<String, String> userAttributes) {
    //        return Optional.ofNullable(userAttributes.get(ORG_FEIDE_DOMAIN)).orElse(null);
    //    }
    //
    //    private static String extractFeideIdentifier(Map<String, String> userAttributes) {
    //        return Optional.ofNullable(userAttributes.get(FEIDE_ID)).orElse(null);
    //    }
    //
    //    @JacocoGenerated
    //    private static CognitoIdentityProviderClient defaultCognitoClient() {
    //        return CognitoIdentityProviderClient.builder()
    //            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
    //            .httpClient(UrlConnectionHttpClient.create())
    //            .region(AWS_REGION)
    //            .build();
    //    }
    //
    //    private AuthenticationInformation collectAuthenticationInformation(String nin,
    //                                                                       String orgFeideDomain,
    //                                                                       String personFeideIdentifier) {
    //        return attempt(() -> new NationalIdentityNumber(nin))
    //            .map(idNumber -> userCreator.collectPersonInformation(idNumber, personFeideIdentifier,
    //            orgFeideDomain))
    //            .map(AuthenticationInformation::new)
    //            .orElseThrow();
    //    }
    //
    //    private List<String> createAccessRights(List<UserDto> usersForPerson) {
    //        final var accessRights = new ArrayList<>(accessRightsPerCustomer(usersForPerson));
    //        final var injectedCustomerIdInAccessRights =
    //            injectCustomerIdInCognitoGroupsToFacilitateOnlineTests(usersForPerson);
    //        accessRights.addAll(injectedCustomerIdInAccessRights);
    //        return accessRights;
    //    }
    //
    //    private List<String> injectCustomerIdInCognitoGroupsToFacilitateOnlineTests(List<UserDto> usersForPerson) {
    //        return (usersForPerson.size() == SINGLE_ITEM_LIST)
    //                   ? createVirtualAccessRightForCustomerIdForUseInTests(usersForPerson)
    //                   : Collections.emptyList();
    //    }
    //
    //    private List<String> createVirtualAccessRightForCustomerIdForUseInTests(List<UserDto> usersForPerson) {
    //        var user = usersForPerson.get(SINGLE_ITEM);
    //        var accessRight = INJECTED_ACCESS_RIGHT_TO_SINGLE_CUSTOMER_ID_IN_COGNITO_GROUPS + AT + user
    //        .getInstitution()
    //            .toString();
    //        return List.of(accessRight);
    //    }
    //
    //    private Collection<String> rolesPerCustomer(List<UserDto> usersForPerson) {
    //        return usersForPerson.stream()
    //            .flatMap(UserDto::generateRoleClaims)
    //            .collect(Collectors.toSet());
    //    }
    //
    //    private void updateCognitoUserAttributes(
    //        CognitoUserPoolPreTokenGenerationEvent input,
    //        AuthenticationInformation authenticationInfo,
    //        Collection<String> accessRights,
    //        Collection<String> roles) {
    //
    //        cognitoClient.adminUpdateUserAttributes(createUpdateUserAttributesRequest(input,
    //                                                                                  authenticationInfo,
    //                                                                                  accessRights,
    //                                                                                  roles));
    //    }
    //
    //    private AdminUpdateUserAttributesRequest createUpdateUserAttributesRequest(
    //        CognitoUserPoolPreTokenGenerationEvent input,
    //        AuthenticationInformation authenticationInfo,
    //        Collection<String> accessRights,
    //        Collection<String> roles) {
    //
    //        return AdminUpdateUserAttributesRequest.builder()
    //            .userPoolId(input.getUserPoolId())
    //            .username(input.getUserName())
    //            .userAttributes(updatedPersonAttributes(authenticationInfo, accessRights, roles))
    //            .build();
    //    }
    //
    //    private Collection<AttributeType> updatedPersonAttributes(AuthenticationInformation authenticationInfo,
    //                                                              Collection<String> accessRights,
    //                                                              Collection<String> roles) {
    //
    //        var allowedCustomersString = createAllowedCustomersString(authenticationInfo.getActiveCustomers());
    //
    //        if (authenticationInfo.personExistsInPersonRegistry()) {
    //            return addClaimsForPeopleRegisteredInPersonRegistry(authenticationInfo,
    //                                                                accessRights,
    //                                                                roles,
    //                                                                allowedCustomersString);
    //        }
    //        return Collections.emptyList();
    //    }
    //
    //    private List<AttributeType> addClaimsForPeopleRegisteredInPersonRegistry(
    //        AuthenticationInformation authenticationInfo,
    //        Collection<String> accessRights,
    //        Collection<String> roles,
    //        String allowedCustomersString) {
    //
    //        var claims = new ArrayList<AttributeType>();
    //        claims.add(createAttribute("custom:firstName", authenticationInfo.extractFirstName()));
    //        claims.add(createAttribute("custom:lastName", authenticationInfo.extractLastName()));
    //        claims.add(createAttribute(ACCESS_RIGHTS_CLAIM, String.join(ELEMENTS_DELIMITER, accessRights)));
    //        claims.add(createAttribute(ROLES_CLAIM, String.join(ELEMENTS_DELIMITER, roles)));
    //        claims.add(createAttribute(ALLOWED_CUSTOMER_CLAIM, allowedCustomersString));
    //        claims.add(createAttribute(PERSON_CRISTIN_ID_CLAIM, authenticationInfo.getCristinPersonId().toString()));
    //        addCustomerSelectionClaimsWhenUserHasOnePossibleLoginOrLoggedInWithFeide(authenticationInfo, claims);
    //        return claims;
    //    }
    //
    //    private void addCustomerSelectionClaimsWhenUserHasOnePossibleLoginOrLoggedInWithFeide(
    //        AuthenticationInformation authenticationInfo,
    //        List<AttributeType> claims) {
    //
    //        authenticationInfo.getCurrentCustomerId()
    //            .ifPresent(customerId -> claims.addAll(customerSelectionClaims(authenticationInfo, customerId)));
    //    }
    //
    //    private List<AttributeType> customerSelectionClaims(AuthenticationInformation authenticationInfo,
    //                                                        String customerId) {
    //
    //        var currentCustomerClaim = createAttribute(CURRENT_CUSTOMER_CLAIM, customerId);
    //        var currentTopLevelOrgClaim =
    //            createAttribute(TOP_ORG_CRISTIN_ID, authenticationInfo.getCurrentCustomer().getCristinId().toString
    //            ());
    //        var usernameClaim = createAttribute(NVA_USERNAME_CLAIM, authenticationInfo.getCurrentUser().getUsername
    //        ());
    //        var personAffiliationClaim =
    //            createAttribute(PERSON_AFFILIATION_CLAIM, authenticationInfo.getCurrentUser().getAffiliation()
    //            .toString());
    //        return List.of(currentCustomerClaim, currentTopLevelOrgClaim, usernameClaim, personAffiliationClaim);
    //    }
    //
    //    private String createAllowedCustomersString(Collection<CustomerDto> allowedCustomers) {
    //        var result = allowedCustomers
    //            .stream()
    //            .map(CustomerDto::getId)
    //            .map(URI::toString)
    //            .collect(Collectors.joining(ELEMENTS_DELIMITER));
    //        return StringUtils.isNotBlank(result)
    //                   ? result
    //                   : EMPTY_ALLOWED_CUSTOMERS;
    //    }
    //
    //    private AttributeType createAttribute(String name, String value) {
    //        return AttributeType.builder().name(name).value(value).build();
    //    }
    //
    //    private void injectAccessRightsToEventResponse(CognitoUserPoolPreTokenGenerationEvent input,
    //                                                   List<String> accessRights) {
    //        input.setResponse(Response.builder()
    //                              .withClaimsOverrideDetails(buildOverrideClaims(accessRights))
    //                              .build());
    //    }
    //
    //    private List<String> accessRightsPerCustomer(List<UserDto> personsUsers) {
    //        return personsUsers.stream()
    //            .map(user -> UserAccessRightForCustomer.fromUser(user, customerService))
    //            .flatMap(Collection::stream)
    //            .map(UserAccessRightForCustomer::toString)
    //            .collect(Collectors.toList());
    //    }
    //
    //    private ClaimsOverrideDetails buildOverrideClaims(List<String> groupsToOverride) {
    //        var groups = GroupConfiguration.builder()
    //            .withGroupsToOverride(groupsToOverride.toArray(String[]::new))
    //            .build();
    //        return ClaimsOverrideDetails.builder()
    //            .withGroupOverrideDetails(groups)
    //            .withClaimsToSuppress(CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC)
    //            .build();
    //    }
}
