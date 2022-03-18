package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.EnvironmentVariables.AWS_REGION;
import static no.unit.nva.cognito.EnvironmentVariables.COGNITO_HOST;
import static no.unit.nva.cognito.NetworkingUtils.CRISTIN_HOST;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.database.IdentityService.defaultIdentityService;
import static no.unit.useraccessservice.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
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
import java.util.Map;
import java.util.Optional;
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
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

public class IdentityServiceEntryUpdateHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    public static final String NIN_FOR_FEIDE_USERS = "custom:feideidnin";
    public static final String NIN_FON_NON_FEIDE_USERS = "custom:nin";
    public static final String FEIDE_ID = "custom:feideid";
    public static final String ORG_FEIDE_DOMAIN = "custom:orgFeideId";
    public static final String NVA_USERNAME = "custom:nvaUsername";
    public static final String BELONGS_TO = "@";
    public static final String ACCESS_RIGTHS_DELIMITER = ",";
    public static final String CURRENT_CUSTOMER_CLAIM = "custom:currentCustomer";
    protected static final String[] CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC = {NIN_FON_NON_FEIDE_USERS,
        NIN_FOR_FEIDE_USERS};
    public static final String AT = "@";
    private final CristinClient cristinClient;
    private final CustomerService customerService;
    private final IdentityService identityService;
    private final CognitoIdentityProviderClient cognitoClient;
    private final BackendJwtTokenRetriever backendJwtTokenRetriever;
    private LambdaLogger logger;

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

        this.logger = context.getLogger();
        var nin = extractNin(input.getRequest().getUserAttributes());
        var feideIdentifier = extractFeideIdentifier(input.getRequest().getUserAttributes());
        var orgFeideDomain = extractOrgFeideDomain(input.getRequest().getUserAttributes());

        var cristinResponse = fetchPersonInformationFromCristin(input, nin);
        var activeCustomers = fetchCustomersForActiveAffiliations(cristinResponse);
        activeCustomers.forEach(customer -> logger.log(customer.toString()));
        var currentCustomer = activeCustomers.stream()
            .filter(customer -> keepCustomerSpecifiedByFeideIfUserLoggedInThroughFeide(customer, orgFeideDomain))
            .collect(SingletonCollector.tryCollect())
            .orElse(fail -> null);
        var usersForPerson = createOrFetchUserEntriesForPerson(cristinResponse, activeCustomers, feideIdentifier);
        var accessRights = accessRightsPerCustomer(usersForPerson);

        injectAccessRightsToEventResponse(input, accessRights);
        updateUserAttributesWithInformationThatAreInterestingInUserInfoEndpoint(input,
                                                                                cristinResponse,
                                                                                currentCustomer,
                                                                                accessRights);

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

    private void updateUserAttributesWithInformationThatAreInterestingInUserInfoEndpoint(
        CognitoUserPoolPreTokenGenerationEvent input,
        CristinPersonResponse cristinResponse,
        CustomerDto customer,
        List<String> accessRights) {

        cognitoClient.adminUpdateUserAttributes(createUpdateUserAttributesRequest(input,
                                                                                  cristinResponse,
                                                                                  customer,
                                                                                  accessRights));
    }

    private AdminUpdateUserAttributesRequest createUpdateUserAttributesRequest(
        CognitoUserPoolPreTokenGenerationEvent input,
        CristinPersonResponse cristinResponse,
        CustomerDto customer,
        List<String> accessRights) {

        return AdminUpdateUserAttributesRequest.builder()
            .userPoolId(input.getUserPoolId())
            .username(input.getUserName())
            .userAttributes(updatedPersonAttributes(cristinResponse, customer, accessRights))
            .build();
    }

    private Collection<AttributeType> updatedPersonAttributes(CristinPersonResponse response,
                                                              CustomerDto customerDto,
                                                              List<String> accessRights) {

        var claims = new ArrayList<AttributeType>();
        claims.add(createAttribute("custom:firstName", response.extractFirstName()));
        claims.add(createAttribute("custom:lastName", response.extractLastName()));
        claims.add(createAttribute("custom:accessRights", String.join(ACCESS_RIGTHS_DELIMITER, accessRights)));
        if (nonNull(customerDto)) {
            claims.add(createAttribute(CURRENT_CUSTOMER_CLAIM, customerDto.getId().toString()));
        }

        return claims;
    }

    private AttributeType createAttribute(String s, String s2) {
        return AttributeType.builder().name(s).value(s2).build();
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

    private List<UserDto> createOrFetchUserEntriesForPerson(CristinPersonResponse cristinResponse,
                                                            Set<CustomerDto> activeCustomers,
                                                            String feideIdentifier) {

        return activeCustomers.stream()
            .map(customer -> createNewUserObject(customer, cristinResponse, feideIdentifier))
            .map(user -> getExistingUserOrCreateNew(user, feideIdentifier))
            .collect(Collectors.toList());
    }

    private UserDto getExistingUserOrCreateNew(UserDto user, String feideIdentifier) {
        return attempt(() -> fetchUserBasedOnCristinIdentifiers(user))
            .or(() -> fetchLegacyUserWithFeideIdentrifer(user, feideIdentifier))
            .or(() -> addUser(user))
            .orElseThrow();
    }

    private UserDto fetchLegacyUserWithFeideIdentrifer(UserDto userWithUpdatedInformation, String feideIdentifier) {
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
                                        CristinPersonResponse cristinResponse,
                                        String feideIdentifier) {

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

    private Set<CustomerDto> fetchCustomersForActiveAffiliations(CristinPersonResponse cristinResponse) {

        return cristinResponse.getAffiliations().stream()
            .filter(CristinAffiliation::isActive)
            .map(CristinAffiliation::getOrganizationUri)
            .map(this::fetchTopLevelOrgUri)
            .map(attempt(customerService::getCustomerByCristinId))
            .flatMap(Try::stream)
            .collect(Collectors.toSet());
    }

    private boolean keepCustomerSpecifiedByFeideIfUserLoggedInThroughFeide(CustomerDto customer,
                                                                           String orgFeideDomain) {
        return userLoggedInWithNin(orgFeideDomain) || orgFeideDomain.equals(customer.getFeideOrganizationDomain());
    }

    private boolean userLoggedInWithNin(String orgFeideDomain) {
        return isNull(orgFeideDomain);
    }

    private URI fetchTopLevelOrgUri(URI orgUri) {
        return attempt(() -> cristinClient.fetchTopLevelOrgUri(orgUri)).orElseThrow();
    }

    private CristinPersonResponse fetchPersonInformationFromCristin(CognitoUserPoolPreTokenGenerationEvent input,
                                                                    String nin) {
        var jwtToken = backendJwtTokenRetriever.fetchJwtToken(input.getUserPoolId());
        return attempt(() -> cristinClient.sendRequestToCristin(jwtToken, nin)).orElseThrow();
    }

    private String extractNin(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(NIN_FOR_FEIDE_USERS))
            .or(() -> Optional.ofNullable(userAttributes.get(NIN_FON_NON_FEIDE_USERS)))
            .orElseThrow();
    }

    private String extractOrgFeideDomain(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(ORG_FEIDE_DOMAIN)).orElse(null);
    }

    private String extractFeideIdentifier(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(FEIDE_ID)).orElse(null);
    }
}
