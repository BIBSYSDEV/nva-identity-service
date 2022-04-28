package no.unit.nva.cognito;

import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ELEMENTS_DELIMITER;
import static no.unit.nva.cognito.CognitoClaims.EMPTY_ALLOWED_CUSTOMERS;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ROLES_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.cognito.NetworkingUtils.AWS_REGION;
import static no.unit.nva.cognito.NetworkingUtils.COGNITO_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.cognito.NetworkingUtils.COGNITO_HOST;
import static no.unit.nva.cognito.NetworkingUtils.COGNITO_ID_KEY;
import static no.unit.nva.cognito.NetworkingUtils.COGNITO_SECRET_KEY;
import static no.unit.nva.cognito.NetworkingUtils.CRISTIN_HOST;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.database.IdentityService.defaultIdentityService;
import static no.unit.useraccessservice.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.ClaimsOverrideDetails;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.GroupConfiguration;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.cognito.cristin.person.CristinClient;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.secrets.SecretsReader;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

public class UserSelectionUponLoginHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    public static final String NIN_FOR_FEIDE_USERS = "custom:feideIdNin";
    public static final String NIN_FON_NON_FEIDE_USERS = "custom:nin";
    public static final String FEIDE_ID = "custom:feideId";
    public static final String ORG_FEIDE_DOMAIN = "custom:orgFeideDomain";
    private final CustomerService customerService;
    private final CognitoIdentityProviderClient cognitoClient;
    private final UserEntriesCreatorForPerson userCreator;

    @JacocoGenerated
    public UserSelectionUponLoginHandler() {
        this(defaultCognitoClient(), defaultAuthorizedBackedClient(), CRISTIN_HOST,
             defaultCustomerService(DEFAULT_DYNAMO_CLIENT), defaultIdentityService(DEFAULT_DYNAMO_CLIENT));
    }

    public UserSelectionUponLoginHandler(CognitoIdentityProviderClient cognitoClient,
                                         AuthorizedBackendClient httpClient,
                                         URI cristinHost,
                                         CustomerService customerService,
                                         IdentityService identityService) {
        this.cognitoClient = cognitoClient;
        this.customerService = customerService;
        var cristinClient = new CristinClient(cristinHost, httpClient);
        this.userCreator = new UserEntriesCreatorForPerson(customerService, cristinClient, identityService);
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {
        var nin = extractNin(input.getRequest().getUserAttributes());
        var orgFeideDomain = extractOrgFeideDomain(input.getRequest().getUserAttributes());
        var personFeideIdentifier = extractFeideIdentifier(input.getRequest().getUserAttributes());

        var authenticationInfo =
            userCreator.collectInformationForPerson(nin,personFeideIdentifier,orgFeideDomain);
        final var usersForPerson = userCreator.createUsers(authenticationInfo);

        final var accessRights = accessRightsPerCustomer(usersForPerson);
        final var roles = rolesPerCustomer(usersForPerson);
        authenticationInfo.updateCurrentCustomer();
        authenticationInfo.updateCurrentUser(usersForPerson);

        injectAccessRightsToEventResponse(input, accessRights);
        updateCognitoUserAttributes(input, authenticationInfo, accessRights, roles);

        return input;
    }

    private static String extractNin(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(NIN_FOR_FEIDE_USERS))
            .or(() -> Optional.ofNullable(userAttributes.get(NIN_FON_NON_FEIDE_USERS)))
            .orElseThrow();
    }

    private static String extractOrgFeideDomain(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(ORG_FEIDE_DOMAIN)).orElse(null);
    }

    private static String extractFeideIdentifier(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(FEIDE_ID)).orElse(null);
    }

    @JacocoGenerated
    private static AuthorizedBackendClient defaultAuthorizedBackedClient() {
        return AuthorizedBackendClient.prepareWithCognitoCredentials(defaultCognitoCredentials());
    }

    @JacocoGenerated
    private static CognitoCredentials defaultCognitoCredentials() {
        var secretsReader = new SecretsReader(SecretsReader.defaultSecretsManagerClient());
        var clientId = secretsReader.fetchSecret(COGNITO_CREDENTIALS_SECRET_NAME, COGNITO_ID_KEY);
        var clientSecret = secretsReader.fetchSecret(COGNITO_CREDENTIALS_SECRET_NAME, COGNITO_SECRET_KEY);
        return new CognitoCredentials(clientId, clientSecret, COGNITO_HOST);
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
        var usernameClaim = createAttribute(NVA_USERNAME_CLAIM, authenticationInfo.getCurrentUser().getUsername());
        var personAffiliationClaim =
            createAttribute(PERSON_AFFILIATION_CLAIM, authenticationInfo.getCurrentUser().getAffiliation().toString());
        return List.of(currentCustomerClaim, currentTopLevelOrgClaim, usernameClaim, personAffiliationClaim);
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
}
