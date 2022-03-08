package no.unit.nva.cognito;

import static no.unit.nva.cognito.NetworkingUtils.APPLICATION_X_WWW_FORM_URLENCODED;
import static no.unit.nva.cognito.NetworkingUtils.AUTHORIZATION_HEADER;
import static no.unit.nva.cognito.NetworkingUtils.AWS_REGION;
import static no.unit.nva.cognito.NetworkingUtils.BACKEND_USER_POOL_CLIENT_NAME;
import static no.unit.nva.cognito.NetworkingUtils.COGNITO_HOST;
import static no.unit.nva.cognito.NetworkingUtils.GRANT_TYPE_CLIENT_CREDENTIALS;
import static no.unit.nva.cognito.NetworkingUtils.JWT_TOKEN_FIELD;
import static no.unit.nva.cognito.NetworkingUtils.formatBasicAuthenticationHeader;
import static no.unit.nva.cognito.NetworkingUtils.standardOauth2TokenEndpoint;
import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.Request;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;

public class IdentityServiceEntryUpdateHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    public static final String EMPTY = "empty";
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
        this.cognitoUri = standardOauth2TokenEndpoint(cognitoHost);
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
        String userAttributesJson = Optional.ofNullable(input.getRequest()).map(Request::getUserAttributes)
                .map(this::mapAsString).orElse(EMPTY);
        context.getLogger().log("userAttributes:" + userAttributesJson);
        String clientMetadataJson = Optional.ofNullable(input.getRequest())
            .map(CognitoUserPoolPreTokenGenerationEvent.Request::getClientMetadata)
            .map(this::mapAsString).orElse(EMPTY);
        context.getLogger().log("userAttributes:" + clientMetadataJson);
        var responseString = Optional.ofNullable(input.getResponse())
            .map(Object::toString).orElse(EMPTY);
        context.getLogger().log("response:" + responseString);
        var clientId = input.getCallerContext().getClientId();
        context.getLogger().log("clientId:" + clientId);
        var userPoolId = input.getUserPoolId();
        context.getLogger().log("userPoolId:" + userPoolId);
        String jwtToken = fetchJwtToken(userPoolId);
        context.getLogger().log("JWT token:" + jwtToken);
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

    @JacocoGenerated
    private String mapAsString(Map<String, String> map)  {
        try {
            return objectMapper.asString(map);
        } catch (IOException e) {
            return "empty";
        }
    }

    private String fetchJwtToken(String userPoolId) {
        HttpRequest postRequest = formatRequestForJwtToken(userPoolId);
        return extractJwtTokenFromResponse(postRequest);
    }

    private String extractJwtTokenFromResponse(HttpRequest postRequest) {
        var body = attempt(() -> sendRequest(postRequest))
            .map(HttpResponse::body)
            .orElseThrow();
        System.out.println("Body:" + body);
        return Try.of(body)
            .map(objectMapper::mapFrom)
            .map(json -> json.get(JWT_TOKEN_FIELD))
            .map(Objects::toString)
            .orElseThrow();
    }

    private HttpRequest formatRequestForJwtToken(String userPoolId) {
        var request = HttpRequest.newBuilder()
            .uri(cognitoUri)
            .setHeader(AUTHORIZATION_HEADER, authenticationString(userPoolId))
            .setHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
            .POST(clientCredentialsAuthType())
            .build();
        System.out.println(request.toString());
        return request;
    }

    private HttpResponse<String> sendRequest(HttpRequest postRequest) throws IOException, InterruptedException {
        return httpClient.send(postRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String authenticationString(String userPoolId) {
        var clientCredentials = fetchUserPoolClientCredentials(userPoolId);
        return formatBasicAuthenticationHeader(clientCredentials.clientId, clientCredentials.clientSecret);
    }

    private ClientCredentials fetchUserPoolClientCredentials(String userPoolId) {
        var clientId = findBackendClientId(userPoolId);

        DescribeUserPoolClientRequest describeBackendClientRequest = DescribeUserPoolClientRequest.builder()
            .userPoolId(userPoolId)
            .clientId(clientId)
            .build();
        var clientSecret= cognitoClient.describeUserPoolClient(describeBackendClientRequest)
            .userPoolClient()
            .clientSecret();
        return new ClientCredentials(clientId, clientSecret);
    }

    private String findBackendClientId(String userPoolId) {
        return cognitoClient
            .listUserPoolClients(ListUserPoolClientsRequest.builder().userPoolId(userPoolId).build())
            .userPoolClients().stream()
            .filter(userPoolClient -> userPoolClient.clientName().equals(BACKEND_USER_POOL_CLIENT_NAME))
            .collect(SingletonCollector.collect())
            .clientId();
    }

    private static class ClientCredentials {

        private final String clientId;
        private final String clientSecret;

        public ClientCredentials(String clientId, String clientSecret) {

            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }
    }
}
