package no.unit.nva.cognito;

import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import java.util.Arrays;
import java.util.List;

public abstract class CognitoCommunicationHandler<I, O> extends ApiGatewayHandler<I, O> {

    public static final String SINGLE_SPACE = " ";
    public static final String EMPTY_STRING = "";
    protected static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AWS_REGION = new Environment().readEnv("AWS_REGION");

    protected CognitoCommunicationHandler(Class<I> iclass) {
        super(iclass);
    }

    @JacocoGenerated
    protected static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(Region.of(AWS_REGION))
            .build();
    }

    protected String extractAccessToken(RequestInfo event) {
        var authorizationHeader = removeBearerTokenPrefix(event);
        return everythingAfterBearerTokenPrefix(authorizationHeader);
    }

    private String everythingAfterBearerTokenPrefix(List<String> authorizationHeader) {
        return String.join(EMPTY_STRING, authorizationHeader.subList(1, authorizationHeader.size()));
    }

    private List<String> removeBearerTokenPrefix(RequestInfo event) {
        return Arrays.asList(event.getAuthHeader().split(SINGLE_SPACE));
    }
}
