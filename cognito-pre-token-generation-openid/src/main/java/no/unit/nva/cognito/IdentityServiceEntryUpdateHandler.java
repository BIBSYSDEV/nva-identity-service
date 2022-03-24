package no.unit.nva.cognito;

import static no.unit.nva.cognito.AuthenticationInformation.NIN_FON_NON_FEIDE_USERS;
import static no.unit.nva.cognito.AuthenticationInformation.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.cognito.EnvironmentVariables.AWS_REGION;
import static no.unit.nva.cognito.EnvironmentVariables.COGNITO_HOST;
import static no.unit.nva.cognito.NetworkingUtils.CRISTIN_HOST;
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
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cognito.cristin.person.CristinAffiliation;
import no.unit.nva.cognito.cristin.person.CristinClient;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

public class IdentityServiceEntryUpdateHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    public static final String BELONGS_TO = "@";
    public static final String ELEMENTS_DELIMITER = ",";
    public static final String CURRENT_CUSTOMER_CLAIM = "custom:customerId";
    public static final String TOP_ORG_CRISTIN_ID = "custom:topOrgCristinId";
    public static final String PERSON_CRISTIN_ID_CLAIM = "custom:cristinId";
    public static final String AT = "@";
    public static final String EMPTY_ALLOWED_CUSTOMERS = "null";
    public static final String ALLOWED_CUSTOMER_CLAIM = "custom:allowedCustomers";
    public static final String ROLES_CLAIM = "custom:roles";
    public static final String ACCESS_RIGHTS_CLAIM = "custom:accessRights";
    protected static final String[] CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC = {NIN_FON_NON_FEIDE_USERS,
        NIN_FOR_FEIDE_USERS};
    private final CristinClient cristinClient;
    private final CustomerService customerService;
    private final IdentityService identityService;
    private final CognitoIdentityProviderClient cognitoClient;
    private final BackendJwtTokenRetriever backendJwtTokenRetriever;

    @JacocoGenerated
    public IdentityServiceEntryUpdateHandler() {
        this(defaultCognitoClient(), HttpClient.newHttpClient(), COGNITO_HOST, CRISTIN_HOST,
             defaultCustomerService(DEFAULT_DYNAMO_CLIENT), defaultIdentityService(DEFAULT_DYNAMO_CLIENT));
    }

    public IdentityServiceEntryUpdateHandler(CognitoIdentityProviderClient cognitoClient,
                                             HttpClient httpClient,
                                             URI cognitoHost,
                                             URI cristinHost,
                                             CustomerService customerService,
                                             IdentityService identityService) {
        this.cognitoClient = cognitoClient;
        this.backendJwtTokenRetriever = new BackendJwtTokenRetriever(cognitoClient, cognitoHost, httpClient);
        this.cristinClient = new CristinClient(cristinHost, httpClient);
        this.customerService = customerService;
        this.identityService = identityService;
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {

        var authenticationInfo = collectInformationForPerson(input);
        var usersForPerson = createOrFetchUserEntriesForPerson(authenticationInfo);
        var accessRights = accessRightsPerCustomer(usersForPerson);
        var roles = rolesPerCustomer(usersForPerson);
        injectAccessRightsToEventResponse(input, accessRights);

        updateCognitoUserAttributes(input, authenticationInfo, accessRights, roles);

        return input;
    }

    @JacocoGenerated
    private static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(AWS_REGION)
            .build();
    }

    private Collection<String> rolesPerCustomer(List<UserDto> usersForPerson) {
        return usersForPerson.stream()
            .flatMap(UserDto::generateRoleClaims)
            .collect(Collectors.toSet());
    }

    private AuthenticationInformation collectInformationForPerson(CognitoUserPoolPreTokenGenerationEvent input) {
        var authenticationInfo = AuthenticationInformation.create(input);

        CristinPersonResponse cristinResponse =
            fetchPersonInformationFromCristin(input, authenticationInfo.getNationalIdentityNumber());
        authenticationInfo.setCristinResponse(cristinResponse);

        var topLevelOrganizations = fetchTopLevelOrgsForBottomLevelOrgs(authenticationInfo);
        authenticationInfo.setTopLevelOrganizations(topLevelOrganizations);

        var activeCustomers = fetchCustomersForActiveAffiliations(authenticationInfo);
        authenticationInfo.setActiveCustomers(activeCustomers);

        authenticationInfo.updateCurrentCustomer();

        return authenticationInfo;
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

        return AdminUpdateUserAttributesRequest.builder()
            .userPoolId(input.getUserPoolId())
            .username(input.getUserName())
            .userAttributes(updatedPersonAttributes(authenticationInfo, accessRights, roles))
            .build();
    }

    private Collection<AttributeType> updatedPersonAttributes(AuthenticationInformation authenticationInfo,
                                                              Collection<String> accessRights,
                                                              Collection<String> roles) {

        var allowedCustomersString = createAllowedCustomersString(authenticationInfo.getActiveCustomers());
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
            .ifPresent(customerId -> claims.addAll(customerSelectionClaims(authenticationInfo, customerId)));
    }

    private List<AttributeType> customerSelectionClaims(AuthenticationInformation authenticationInfo,
                                                        String customerId) {

        var currentCustomerClaim = createAttribute(CURRENT_CUSTOMER_CLAIM, customerId);
        var currentTopLevelOrgClaim =
            createAttribute(TOP_ORG_CRISTIN_ID, authenticationInfo.getCurrentCustomer().getCristinId().toString());
        return List.of(currentCustomerClaim, currentTopLevelOrgClaim);
    }

    private String createAllowedCustomersString(Collection<CustomerDto> allowedCustomers) {
        var result = allowedCustomers
            .stream()
            .map(CustomerDto::getId)
            .map(URI::toString)
            .collect(Collectors.joining(ELEMENTS_DELIMITER));
        return StringUtils.isNotBlank(result)
                   ? result
                   : EMPTY_ALLOWED_CUSTOMERS;
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

    private List<String> accessRightsPerCustomer(List<UserDto> personsUsers) {
        return personsUsers.stream()
            .map(user -> CustomerAccessRight.fromUser(user, customerService))
            .flatMap(Collection::stream)
            .map(CustomerAccessRight::asStrings)
            .flatMap(Collection::stream)
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

    private List<UserDto> createOrFetchUserEntriesForPerson(AuthenticationInformation authenticationInformation) {

        return authenticationInformation.getActiveCustomers().stream()
            .map(customer -> createNewUserObject(customer, authenticationInformation))
            .map(user -> getExistingUserOrCreateNew(user, authenticationInformation))
            .collect(Collectors.toList());
    }

    private UserDto getExistingUserOrCreateNew(UserDto user, AuthenticationInformation authenticationInformation) {
        return attempt(() -> fetchUserBasedOnCristinIdentifiers(user))
            .or(() -> fetchLegacyUserWithFeideIdentifier(user, authenticationInformation.getFeideIdentifier()))
            .or(() -> addUser(user))
            .orElseThrow();
    }

    private UserDto fetchLegacyUserWithFeideIdentifier(UserDto userWithUpdatedInformation, String feideIdentifier) {
        var queryObject = UserDto.newBuilder().withUsername(feideIdentifier).build();
        var savedUser = identityService.getUser(queryObject);
        var updatedUser = savedUser.copy()
            .withFeideIdentifier(feideIdentifier)
            .withCristinId(userWithUpdatedInformation.getCristinId())
            .withInstitutionCristinId(userWithUpdatedInformation.getInstitutionCristinId())
            .build();
        identityService.updateUser(updatedUser);
        return updatedUser;
    }

    private UserDto fetchUserBasedOnCristinIdentifiers(UserDto user) {
        return identityService.getUserByCristinIdAndCristinOrgId(user.getCristinId(), user.getInstitutionCristinId());
    }

    private UserDto addUser(UserDto user) {
        identityService.addUser(user);
        return user;
    }

    private UserDto createNewUserObject(CustomerDto customer,
                                        AuthenticationInformation authenticationInformation) {

        var cristinResponse = authenticationInformation.getCristinPersonResponse();
        var feideIdentifier = authenticationInformation.getFeideIdentifier();
        var user = UserDto.newBuilder()
            .withUsername(formatUsername(cristinResponse, customer))
            .withFeideIdentifier(feideIdentifier)
            .withInstitution(customer.getId())
            .withGivenName(cristinResponse.extractFirstName())
            .withFamilyName(cristinResponse.extractLastName())
            .withCristinId(cristinResponse.getCristinId())
            .withCristinId(cristinResponse.getCristinId())
            .withInstitutionCristinId(customer.getCristinId());

        return user.build();
    }

    private String formatUsername(CristinPersonResponse cristinResponse, CustomerDto customer) {
        var personIdentifier = cristinResponse.getPersonsCristinIdentifier().getValue();
        var customerIdentifier = UriWrapper.fromUri(customer.getCristinId()).getLastPathElement();
        return personIdentifier + AT + customerIdentifier;
    }

    private Set<CustomerDto> fetchCustomersForActiveAffiliations(AuthenticationInformation authenticationInformation) {

        return authenticationInformation.getTopLevelOrganizations()
            .stream()
            .map(attempt(customerService::getCustomerByCristinId))
            .flatMap(Try::stream)
            .collect(Collectors.toSet());
    }

    private List<URI> fetchTopLevelOrgsForBottomLevelOrgs(
        AuthenticationInformation authenticationInformation) {
        return authenticationInformation.getCristinPersonResponse().getAffiliations().stream()
            .filter(CristinAffiliation::isActive)
            .map(CristinAffiliation::getOrganizationUri)
            .map(this::fetchTopLevelOrgUri)
            .collect(Collectors.toList());
    }

    private URI fetchTopLevelOrgUri(URI bottomeLevelOrg) {
        return attempt(() -> cristinClient.fetchTopLevelOrgUri(bottomeLevelOrg))
            .orElseThrow();
    }

    private CristinPersonResponse fetchPersonInformationFromCristin(
        CognitoUserPoolPreTokenGenerationEvent input, String nin) {
        var jwtToken = backendJwtTokenRetriever.fetchJwtToken(input.getUserPoolId());
        return attempt(() -> cristinClient.sendRequestToCristin(jwtToken, nin)).orElseThrow();
    }
}
