package no.unit.nva.cognito;

import static no.unit.nva.cognito.NetworkingUtils.APPLICATION_X_WWW_FORM_URLENCODED;
import static no.unit.nva.cognito.NetworkingUtils.AUTHORIZATION_HEADER;
import static no.unit.nva.cognito.NetworkingUtils.BACKEND_USER_POOL_CLIENT_NAME;
import static no.unit.nva.cognito.NetworkingUtils.GRANT_TYPE_CLIENT_CREDENTIALS;
import static no.unit.nva.cognito.NetworkingUtils.JWT_TOKEN_FIELD;
import static nva.commons.core.attempt.Try.attempt;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;

class RequestAuthorizer {

    private final CognitoIdentityProviderClient cognitoClient;
    private final URI cognitoUri;
    private final HttpClient httpClient;

    protected RequestAuthorizer(CognitoIdentityProviderClient cognitoClient, URI cognitoHost, HttpClient httpClient) {
        this.cognitoClient = cognitoClient;
        this.cognitoUri = standardOauth2TokenEndpoint(cognitoHost);
        this.httpClient = httpClient;
    }

    public static URI standardOauth2TokenEndpoint(URI cognitoHost) {
        return new UriWrapper(cognitoHost).addChild("oauth2").addChild("token").getUri();
    }

    public String fetchJwtToken(String userPoolId) {
        HttpRequest postRequest = formatRequestForJwtToken(userPoolId);
        return sendRequestAndExtractToken(postRequest);
    }



    private HttpRequest formatRequestForJwtToken(String userPoolId) {
        return HttpRequest.newBuilder()
            .uri(cognitoUri)
            .setHeader(AUTHORIZATION_HEADER, authenticationString(userPoolId))
            .setHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
            .POST(clientCredentialsAuthType())
            .build();
    }
    private static HttpRequest.BodyPublisher clientCredentialsAuthType() {
        var queryParameters = UriWrapper.fromHost("notimportant")
            .addQueryParameters(GRANT_TYPE_CLIENT_CREDENTIALS).getUri().getRawQuery();
        return HttpRequest.BodyPublishers.ofString(queryParameters);
    }

    private String authenticationString(String userPoolId) {
        var clientCredentials = fetchUserPoolClientCredentials(userPoolId);
        return formatBasicAuthenticationHeader(clientCredentials.clientId, clientCredentials.clientSecret);
    }

    private static String formatBasicAuthenticationHeader(String clientId, String clientSecret) {
        return attempt(() -> String.format("%s:%s", clientId, clientSecret))
            .map(str -> Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8)))
            .map(credentials -> "Basic " + credentials)
            .orElseThrow();
    }

    private String sendRequestAndExtractToken(HttpRequest postRequest) {
        return attempt(() -> sendRequest(postRequest))
            .map(HttpResponse::body)
            .map(JSON.std::mapFrom)
            .map(json -> json.get(JWT_TOKEN_FIELD))
            .map(Objects::toString)
            .orElseThrow();
    }

    private HttpResponse<String> sendRequest(HttpRequest postRequest) throws IOException, InterruptedException {
        return httpClient.send(postRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private ClientCredentials fetchUserPoolClientCredentials(String userPoolId) {
        var clientId = findBackendClientId(userPoolId);
        String clientSecret = fetchBackendClientSecret(userPoolId, clientId);
        return new ClientCredentials(clientId, clientSecret);
    }

    private String fetchBackendClientSecret(String userPoolId, String clientId) {
        var describeBackendClientRequest = DescribeUserPoolClientRequest.builder()
            .userPoolId(userPoolId)
            .clientId(clientId)
            .build();
        return cognitoClient.describeUserPoolClient(describeBackendClientRequest)
            .userPoolClient()
            .clientSecret();
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
