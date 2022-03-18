package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.util.Arrays;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserAttributesRequest;

@JacocoGenerated
public class UserSelectionHandler extends ApiGatewayHandlerV2<Void, Void> {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AWS_REGION = new Environment().readEnv("AWS_REGION");
    private final CognitoIdentityProviderClient cognito;

    public UserSelectionHandler() {
        this(defaultCognitoClient());
    }

    public UserSelectionHandler(CognitoIdentityProviderClient cognito) {
        super();
        this.cognito = cognito;
    }

    @Override
    protected Integer getSuccessStatusCode(String body, Void output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected Void processInput(String body, APIGatewayProxyRequestEvent input, Context context) {
        var authorizationHeader = Arrays.asList(input.getHeaders().get(AUTHORIZATION_HEADER).split(" "));
        var accessToken = String.join("", authorizationHeader.subList(1, authorizationHeader.size()));
        AttributeType userAttribute =
            AttributeType.builder().name("custom:currentCustomer").value("https://wwww.olllllllalalala.com").build();
        UpdateUserAttributesRequest request = UpdateUserAttributesRequest.builder()
            .accessToken(accessToken)
            .userAttributes(userAttribute).build();
        cognito.updateUserAttributes(request);
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
}
