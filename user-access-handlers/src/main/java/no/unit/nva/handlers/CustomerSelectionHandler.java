package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.apigatewayv2.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserAttributesRequest;

@JacocoGenerated
public class CustomerSelectionHandler extends ApiGatewayHandlerV2<Void, Void> {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CURRENT_CUSTOMER_CLAIM = "custom:currentCustomer";
    public static final String ALLOWED_CUSTOMERS_CLAIM = "custom:allowedCustomers";
    public static final String EMPTY_STRING = "";
    private static final String AWS_REGION = new Environment().readEnv("AWS_REGION");
    private static final Logger logger = LoggerFactory.getLogger(CustomerSelectionHandler.class);
    private final CognitoIdentityProviderClient cognito;

    public CustomerSelectionHandler() {
        this(defaultCognitoClient());
    }

    public CustomerSelectionHandler(CognitoIdentityProviderClient cognito) {
        super();
        this.cognito = cognito;
    }

    @Override
    protected Integer getSuccessStatusCode(String body, Void output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected Void processInput(String body, APIGatewayProxyRequestEvent event, Context context) {
        var input = CustomerSelection.fromJson(body);
        var accessToken = extractAccessToken(event);
        var allowedCustomers = extractAllowedCustomers(accessToken);
        logger.info("AllowedCustomers:{}", allowedCustomers);
        logger.info("SelectedCustomer:{}", input.getCustomerId());
        validateInput(allowedCustomers, input.getCustomerId());
        updateCognitoUserEntryAttributes(input, accessToken);
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

    private void updateCognitoUserEntryAttributes(CustomerSelection input, String accessToken) {
        var userAttribute =
            AttributeType.builder().name(CURRENT_CUSTOMER_CLAIM).value(input.getCustomerId().toString()).build();
        UpdateUserAttributesRequest request = UpdateUserAttributesRequest.builder()
            .accessToken(accessToken)
            .userAttributes(userAttribute).build();
        cognito.updateUserAttributes(request);
    }

    private void validateInput(String allowedCustomers, URI customerId) {
        var desiredCustomerIdString = customerId.toString().toLowerCase(Locale.getDefault());
        if (desiredCustomerIsNotAllowed(allowedCustomers, desiredCustomerIdString)) {
            throw new ForbiddenException();
        }
    }

    private boolean desiredCustomerIsNotAllowed(String allowedCustomers, String desiredCustomerIdString) {
        return !allowedCustomers.toLowerCase(Locale.getDefault()).contains(desiredCustomerIdString);
    }

    private String extractAllowedCustomers(String accessToken) {
        var user = cognito.getUser(GetUserRequest.builder().accessToken(accessToken).build());
        return user.userAttributes().stream()
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
