package no.unit.nva.cognito;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import no.unit.useraccessservice.database.DatabaseConfig;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.apigatewayv2.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserAttributesRequest;

@JacocoGenerated
public class CustomerSelectionHandler extends ApiGatewayHandlerV2<Void, Void> {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CURRENT_CUSTOMER_CLAIM = "custom:customerId";
    public static final String ALLOWED_CUSTOMERS_CLAIM = "custom:allowedCustomers";
    public static final String PERSON_ID_CLAIM = "custom:cristinId";
    public static final String NVA_USERNAME_CLAIM = "custom:nvaUsername";

    public static final String EMPTY_STRING = "";
    private static final String AWS_REGION = new Environment().readEnv("AWS_REGION");
    public static final String AT = "@";
    private final CognitoIdentityProviderClient cognito;
    private final CustomerService customerService;

    public CustomerSelectionHandler() {
        this(defaultCognitoClient(), defaultCustomerService(DatabaseConfig.DEFAULT_DYNAMO_CLIENT));
    }

    public CustomerSelectionHandler(CognitoIdentityProviderClient cognito, CustomerService customerService) {
        super();
        this.cognito = cognito;
        this.customerService = customerService;
    }

    @Override
    protected Integer getSuccessStatusCode(String body, Void output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected Void processInput(String body, APIGatewayProxyRequestEvent event, Context context) {
        var input = CustomerSelection.fromJson(body);
        var accessToken = extractAccessToken(event);
        var userAttributes = fetchUserInfo(accessToken).userAttributes();

        validateInput(userAttributes, input.getCustomerId());
        updateCognitoUserEntryAttributes(input, userAttributes, accessToken);
        return null;
    }

    private String extractCristinPersonId(List<AttributeType> userAttributes) {
        return userAttributes.stream()
            .filter(attribute -> PERSON_ID_CLAIM.equals(attribute.name()))
            .map(AttributeType::value)
            .collect(SingletonCollector.collect());
    }

    private GetUserResponse fetchUserInfo(String accessToken) {
        return cognito.getUser(GetUserRequest.builder().accessToken(accessToken).build());
    }

    @JacocoGenerated
    private static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(Region.of(AWS_REGION))
            .build();
    }

    private void updateCognitoUserEntryAttributes(CustomerSelection customerSelection,
                                                  List<AttributeType> userAttributes,
                                                  String accessToken) {
        var selectedCustomer =
            createCustomerSelectionClaim(customerSelection);

        var nvaUsername =
            createUsernameClaim(customerSelection, userAttributes);

        UpdateUserAttributesRequest request = UpdateUserAttributesRequest.builder()
            .accessToken(accessToken)
            .userAttributes(selectedCustomer, nvaUsername)
            .build();
        cognito.updateUserAttributes(request);
    }

    private AttributeType createCustomerSelectionClaim(CustomerSelection customerSelection) {
        return AttributeType.builder().name(CURRENT_CUSTOMER_CLAIM)
            .value(customerSelection.getCustomerId().toString())
            .build();
    }

    private AttributeType createUsernameClaim(CustomerSelection customerSelection, List<AttributeType> userAttributes) {
        return AttributeType.builder().name(NVA_USERNAME_CLAIM)
            .value(constructUserName(customerSelection, userAttributes))
            .build();
    }

    private String constructUserName(CustomerSelection customerSelection, List<AttributeType> userAttributes) {
        var cristinPersonIdentifier = extractCristinPersonIdentifier(userAttributes);
        var customerCristinIdentifier =
            fetchCustomerCrstinIdentifier(customerSelection);
        return cristinPersonIdentifier + AT + customerCristinIdentifier;
    }

    private String extractCristinPersonIdentifier(List<AttributeType> userAttributes) {
        return attempt(() -> extractCristinPersonId(userAttributes))
            .map(UriWrapper::fromUri)
            .map(UriWrapper::getLastPathElement)
            .orElseThrow();
    }

    private String fetchCustomerCrstinIdentifier(CustomerSelection customerSelection) {
        return attempt(() -> customerService.getCustomer(customerSelection.getCustomerId()).getCristinId())
            .map(UriWrapper::fromUri)
            .map(UriWrapper::getLastPathElement)
            .orElseThrow();
    }

    private void validateInput(List<AttributeType> userAttributes, URI customerId) {
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

    private String extractAccessToken(APIGatewayProxyRequestEvent event) {
        var authorizationHeader = Arrays.asList(event.getHeaders().get(AUTHORIZATION_HEADER).split(" "));
        return String.join("", authorizationHeader.subList(1, authorizationHeader.size()));
    }
}
