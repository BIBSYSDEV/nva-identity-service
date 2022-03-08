package no.unit.nva.cognito;

import static no.unit.nva.cognito.NetworkingUtils.APPLICATION_X_WWW_FORM_URLENCODED;
import static no.unit.nva.cognito.NetworkingUtils.AUTHORIZATION_HEADER;
import static no.unit.nva.cognito.NetworkingUtils.AWS_REGION;
import static no.unit.nva.cognito.NetworkingUtils.COGNITO_HOST;
import static no.unit.nva.cognito.NetworkingUtils.GRANT_TYPE_CLIENT_CREDENTIALS;
import static no.unit.nva.cognito.NetworkingUtils.JWT_TOKEN_FIELD;
import static no.unit.nva.cognito.NetworkingUtils.formatBasicAuthenticationHeader;
import static no.unit.nva.cognito.NetworkingUtils.standardOauth2Token;
import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;

public class IdentityServiceEntryUpdateHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    private final CognitoIdentityProviderClient cognitoClient;
    private final HttpClient httpClient;
    private final URI cognitoUri;

    @JacocoGenerated
    public IdentityServiceEntryUpdateHandler() {
        this(defaultCognitoClient(), HttpClient.newHttpClient(), defaultCognitoUri());
    }

    public IdentityServiceEntryUpdateHandler(CognitoIdentityProviderClient cognitoClient,
                                             HttpClient httpClient,
                                             URI cognitoHost) {
        this.cognitoClient = cognitoClient;
        this.cognitoUri = standardOauth2Token(cognitoHost);
        this.httpClient = httpClient;
    }

    public static HttpRequest.BodyPublisher clientCredentialsAuthType() {
        var queryParameters = UriWrapper.fromHost("notimportant")
            .addQueryParameters(GRANT_TYPE_CLIENT_CREDENTIALS).getUri().getRawQuery();
        return HttpRequest.BodyPublishers.ofString(queryParameters);
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {
        context.getLogger().log(input.toString());
        String clientId = input.getCallerContext().getClientId();
        String userPoolId = input.getUserPoolId();
        String jwtToken = fetchJwtToken(userPoolId, clientId);
        context.getLogger().log(jwtToken);
        return input;
    }

    @JacocoGenerated
    private static URI defaultCognitoUri() {
        try {
            return new URI("https", COGNITO_HOST, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @JacocoGenerated
    private static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(AWS_REGION)
            .build();
    }

    private String fetchJwtToken(String userPoolId, String clientId) {
        HttpRequest postRequest = formatRequestForJwtToken(userPoolId, clientId);
        return extractJwtTokenFromResponse(postRequest);
    }

    private String extractJwtTokenFromResponse(HttpRequest postRequest) {
        return attempt(() -> sendRequest(postRequest))
            .map(HttpResponse::body)
            .map(objectMapper::mapFrom)
            .map(json -> json.get(JWT_TOKEN_FIELD))
            .map(Objects::toString)
            .orElseThrow();
    }

    private HttpRequest formatRequestForJwtToken(String userPoolId, String clientId) {
        return HttpRequest.newBuilder()
            .uri(cognitoUri)
            .setHeader(AUTHORIZATION_HEADER, authenticationString(userPoolId, clientId))
            .setHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
            .POST(clientCredentialsAuthType())
            .build();
    }

    private HttpResponse<String> sendRequest(HttpRequest postRequest) throws IOException, InterruptedException {
        return httpClient.send(postRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String authenticationString(String userPoolId, String clientId) {
        var clientSecret = fetchUserPoolClientSecret(userPoolId, clientId);
        return formatBasicAuthenticationHeader(clientId, clientSecret);
    }

    private String fetchUserPoolClientSecret(String userPoolId, String clientId) {
        return fetchClientSecret(userPoolId, clientId);
    }

    private String fetchClientSecret(String pool, String clientId) {
        DescribeUserPoolClientRequest describeBackendClientRequest = DescribeUserPoolClientRequest.builder()
            .userPoolId(pool)
            .clientId(clientId)
            .build();
        return cognitoClient.describeUserPoolClient(describeBackendClientRequest)
            .userPoolClient()
            .clientSecret();
    }
}
