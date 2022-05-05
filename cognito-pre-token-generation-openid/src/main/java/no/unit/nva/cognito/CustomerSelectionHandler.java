package no.unit.nva.cognito;

import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.useraccessservice.database.DatabaseConfig;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserAttributesRequest;

@JacocoGenerated
public class CustomerSelectionHandler extends ApiGatewayHandler<CustomerSelection, Void> {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String EMPTY_STRING = "";
    private static final String AWS_REGION = new Environment().readEnv("AWS_REGION");
    private final CognitoIdentityProviderClient cognito;
    private final CustomerService customerService;
    private final IdentityService identityService;

    public CustomerSelectionHandler() {
        this(defaultCognitoClient(),
             defaultCustomerService(DatabaseConfig.DEFAULT_DYNAMO_CLIENT),
             IdentityService.defaultIdentityService(DatabaseConfig.DEFAULT_DYNAMO_CLIENT)
        );
    }

    public CustomerSelectionHandler(CognitoIdentityProviderClient cognito,
                                    CustomerService customerService,
                                    IdentityService identityService) {
        super(CustomerSelection.class);
        this.cognito = cognito;
        this.customerService = customerService;
        this.identityService = identityService;
    }

    @Override
    protected Integer getSuccessStatusCode(CustomerSelection body, Void output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected Void processInput(CustomerSelection input, RequestInfo event, Context context) throws ForbiddenException {

        var accessToken = extractAccessToken(event);
        var userAttributes = fetchUserInfo(accessToken).userAttributes();
        validateInput(userAttributes, input.getCustomerId());
        updateCognitoUserEntryAttributes(input, userAttributes, accessToken);
        return null;
    }

    @JacocoGenerated
    private static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(Region.of(AWS_REGION))
            .build();
    }

    private URI extractCristinPersonId(List<AttributeType> userAttributes) {
        return userAttributes.stream()
            .filter(attribute -> PERSON_ID_CLAIM.equals(attribute.name()))
            .map(AttributeType::value)
            .map(URI::create)
            .collect(SingletonCollector.collect());
    }

    private GetUserResponse fetchUserInfo(String accessToken) {
        return cognito.getUser(GetUserRequest.builder().accessToken(accessToken).build());
    }

    private void updateCognitoUserEntryAttributes(CustomerSelection customerSelection,
                                                  List<AttributeType> userAttributes,
                                                  String accessToken) {
        var user = fetchUser(customerSelection, userAttributes);

        var selectedCustomerCustomClaim = createCustomerSelectionClaim(user);
        var nvaUsernameClaim = createUsernameClaim(user);
        var selectedCustomerCristinId = crateCustomerCristinIdClaim(user);
        var userAffiliation = createUserAffiliationClaim(user);

        var request = UpdateUserAttributesRequest.builder()
            .accessToken(accessToken)
            .userAttributes(selectedCustomerCustomClaim, nvaUsernameClaim, selectedCustomerCristinId, userAffiliation)
            .build();
        cognito.updateUserAttributes(request);
    }

    private AttributeType createUserAffiliationClaim(UserDto user) {
        return createAttribute(PERSON_AFFILIATION_CLAIM, user.getAffiliation());
    }

    private AttributeType crateCustomerCristinIdClaim(UserDto userDto) {
        return createAttribute(TOP_ORG_CRISTIN_ID, userDto.getInstitutionCristinId());
    }

    private AttributeType createUsernameClaim(UserDto user) {
        return createAttribute(NVA_USERNAME_CLAIM, user.getUsername());
    }

    private AttributeType createCustomerSelectionClaim(UserDto user) {
        return createAttribute(CURRENT_CUSTOMER_CLAIM, user.getInstitution());
    }

    private AttributeType createAttribute(String attributeName, URI attributeValue) {
        return AttributeType.builder().name(attributeName)
            .value(attributeValue.toString())
            .build();
    }

    private AttributeType createAttribute(String attributeName, String attributeValue) {
        return AttributeType.builder().name(attributeName)
            .value(attributeValue)
            .build();
    }

    private UserDto fetchUser(CustomerSelection customerSelection, List<AttributeType> userAttributes) {
        var cristinPersonId = extractCristinPersonId(userAttributes);
        var customerCristinId =
            fetchCustomerCristinId(customerSelection);
        return identityService.getUserByPersonCristinIdAndCustomerCristinId(cristinPersonId, customerCristinId);
    }

    private URI fetchCustomerCristinId(CustomerSelection customerSelection) {
        return attempt(() -> customerService.getCustomer(customerSelection.getCustomerId()).getCristinId())
            .orElseThrow();
    }

    private void validateInput(List<AttributeType> userAttributes, URI customerId) throws ForbiddenException {
        var allowedCustomers = extractAllowedCustomers(userAttributes);
        var desiredCustomerIdString = customerId.toString().toLowerCase(Locale.getDefault());
        if (desiredCustomerIsNotAllowed(allowedCustomers, desiredCustomerIdString)) {
            throw new ForbiddenException();
        }
    }

    private boolean desiredCustomerIsNotAllowed(String allowedCustomers, String desiredCustomerIdString) {
        return !allowedCustomers.toLowerCase(Locale.getDefault()).contains(desiredCustomerIdString);
    }

    private String extractAllowedCustomers(List<AttributeType> userAttributes) {
        return userAttributes.stream()
            .filter(attribute -> ALLOWED_CUSTOMERS_CLAIM.equals(attribute.name()))
            .collect(SingletonCollector.tryCollect())
            .map(AttributeType::value)
            .orElse(fail -> EMPTY_STRING);
    }

    private String extractAccessToken(RequestInfo event) {
        var authorizationHeader = removeBearerTokenPrefix(event);
        return everythingAfterBearerTokenPrefix(authorizationHeader);
    }

    private String everythingAfterBearerTokenPrefix(List<String> authorizationHeader) {
        return String.join("", authorizationHeader.subList(1, authorizationHeader.size()));
    }

    private List<String> removeBearerTokenPrefix(RequestInfo event) {
        return Arrays.asList(event.getAuthHeader().split(" "));
    }
}
