package no.unit.nva.cognito;

import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public abstract class CognitoCommunicationHandler<I, O> extends ApiGatewayHandler<I, O> {

    private static final String AWS_REGION = new Environment().readEnv("AWS_REGION");

    protected CognitoCommunicationHandler(Class<I> iclass, Environment environment) {
        super(iclass, environment);
    }

    @JacocoGenerated
    protected static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(Region.of(AWS_REGION))
            .build();
    }
}
