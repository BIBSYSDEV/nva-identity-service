package no.unit.nva;

import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeResourceServerRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ScopeDoesNotExistException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.TokenValidityUnitsType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static no.unit.nva.useraccessservice.constants.ServiceConstants.ENVIRONMENT;
import static nva.commons.core.attempt.Try.attempt;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType.ALLOW_ADMIN_USER_PASSWORD_AUTH;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType.ALLOW_CUSTOM_AUTH;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType.ALLOW_REFRESH_TOKEN_AUTH;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType.ALLOW_USER_SRP_AUTH;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.TimeUnitsType.DAYS;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.TimeUnitsType.MINUTES;

public class CognitoService {
    public static final String AWS_REGION = ENVIRONMENT.readEnv("AWS_REGION");
    public static final String EXTERNAL_SCOPE_IDENTIFIER = new Environment().readEnv("EXTERNAL_SCOPE_IDENTIFIER");
    private static final String EXTERNAL_USER_POOL_ID = new Environment().readEnv("EXTERNAL_USER_POOL_ID");
    private final CognitoIdentityProviderClient client;

    public CognitoService(CognitoIdentityProviderClient client) {
        this.client = client;
    }

    @JacocoGenerated
    public static CognitoService defaultCognitoService() {
        var client = CognitoIdentityProviderClient.builder()
                .httpClient(UrlConnectionHttpClient.builder().build())
                .region(Region.of(AWS_REGION))
                .build();
        return new CognitoService(client);
    }

    public CreateUserPoolClientResponse createUserPoolClient(String appClientName, List<String> scopes)
            throws BadRequestException {
        return attempt(() -> client.createUserPoolClient(buildCreateCognitoClientRequest(appClientName, scopes)))
                .orElseThrow(fail -> handleUserPoolCreationFailure(fail.getException(), scopes));


    }

    private TokenValidityUnitsType getDefaultTokenValidityUnits() {
        return TokenValidityUnitsType.builder()
                .refreshToken(DAYS)
                .accessToken(MINUTES)
                .idToken(MINUTES)
                .build();
    }

    private CreateUserPoolClientRequest buildCreateCognitoClientRequest(String appClientName,
                                                                        Collection<String> scopes) {
        return CreateUserPoolClientRequest.builder()
                .userPoolId(EXTERNAL_USER_POOL_ID)
                .clientName(appClientName)
                .generateSecret(true)
                .allowedOAuthScopes(scopes)
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

    private DescribeResourceServerRequest buildDescribeResourceServerRequest() {
        return DescribeResourceServerRequest.builder()
                .userPoolId(EXTERNAL_USER_POOL_ID)
                .identifier(EXTERNAL_SCOPE_IDENTIFIER)
                .build();
    }

    private BadRequestException handleUserPoolCreationFailure(Exception exception, Collection<String> scopes) {
        if (exception instanceof ScopeDoesNotExistException) {
            var describeResourceServerRequest = buildDescribeResourceServerRequest();

            var validScopes =
                    client.describeResourceServer(describeResourceServerRequest)
                            .resourceServer()
                            .scopes()
                            .stream()
                            .map(scopeType -> EXTERNAL_SCOPE_IDENTIFIER + "/" + scopeType.scopeName())
                            .collect(Collectors.toSet());

            return new BadRequestException("Unknown scopes: " + getUnknownScopes(validScopes, scopes));
        }
        throw new RuntimeException(exception.getMessage());
    }

    private Collection<String> getUnknownScopes(Collection<String> knownScopes, Collection<String> usedScopes) {
        var unknownScopes = new ArrayList(usedScopes);
        unknownScopes.removeAll(knownScopes);
        return unknownScopes;
    }
}
