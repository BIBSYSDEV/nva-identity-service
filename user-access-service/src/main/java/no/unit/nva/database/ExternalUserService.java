package no.unit.nva.database;

import static no.unit.nva.useraccessservice.constants.ServiceConstants.ENVIRONMENT;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType.ALLOW_ADMIN_USER_PASSWORD_AUTH;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType.ALLOW_CUSTOM_AUTH;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType.ALLOW_REFRESH_TOKEN_AUTH;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType.ALLOW_USER_SRP_AUTH;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.TimeUnitsType.DAYS;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.TimeUnitsType.MINUTES;
import java.util.Collection;
import java.util.List;
import no.unit.nva.useraccessservice.model.CreateExternalUserResponse;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.TokenValidityUnitsType;

public class ExternalUserService {

    private final CognitoIdentityProviderClient client;
    public static final String AWS_REGION = ENVIRONMENT.readEnv("AWS_REGION");
    private static final String EXTERNAL_USER_POOL_ID = new Environment().readEnv("EXTERNAL_USER_POOL_ID");
    private static final String EXTERNAL_USER_POOL_URL = new Environment().readEnv("EXTERNAL_USER_POOL_URL");

    public ExternalUserService(CognitoIdentityProviderClient client) {
        this.client = client;
    }

    private Collection<String> getDefaultScopes() {
        return List.of(
            "https://api.nva.unit.no/scopes/user/read",
            "https://api.nva.unit.no/scopes/user/write"
        );
    }

    private CreateUserPoolClientRequest buildCreateCognitoClientRequest(String appClientName) {
        return CreateUserPoolClientRequest.builder()
                   .userPoolId(EXTERNAL_USER_POOL_ID)
                   .clientName(appClientName)
                   .generateSecret(true)
                   .allowedOAuthScopes(getDefaultScopes())
                   .allowedOAuthFlowsWithStrings("client_credentials")
                   .allowedOAuthFlowsUserPoolClient(true)
                   .explicitAuthFlows(
                       List.of(
                           ALLOW_ADMIN_USER_PASSWORD_AUTH,
                           ALLOW_CUSTOM_AUTH,
                           ALLOW_REFRESH_TOKEN_AUTH,
                           ALLOW_USER_SRP_AUTH)
                   )
                   .tokenValidityUnits(getDefaultTokenValidityUnits())
                   .refreshTokenValidity(30)
                   .accessTokenValidity(15)
                   .idTokenValidity(15)
                   .build();

    }

    private TokenValidityUnitsType getDefaultTokenValidityUnits() {
        return TokenValidityUnitsType.builder()
                   .refreshToken(DAYS)
                   .accessToken(MINUTES)
                   .idToken(MINUTES)
                   .build();
    }

    public CreateExternalUserResponse createNewExternalUserClient(String appClientName) {
        var response = client.createUserPoolClient(buildCreateCognitoClientRequest(appClientName));
        return createCogntioCredentialsDtoFromResponse(response);

    }

    private CreateExternalUserResponse createCogntioCredentialsDtoFromResponse(CreateUserPoolClientResponse response) {
        var userPoolClient = response.userPoolClient();
        return new CreateExternalUserResponse(
            userPoolClient.clientId(),
            userPoolClient.clientSecret(),
            EXTERNAL_USER_POOL_URL
        );
    }

    @JacocoGenerated
    public static ExternalUserService defaultExternalUserClientService() {
        var client = CognitoIdentityProviderClient.builder()
                                                   .httpClient(UrlConnectionHttpClient.builder().build())
                                                   .region(Region.of(AWS_REGION))
                                                   .build();
        return new ExternalUserService(client);
    }
}
