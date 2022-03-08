package no.unit.nva.cognito;

import static no.unit.nva.cognito.NetworkingUtils.APPLICATION_X_WWW_FORM_URLENCODED;
import static no.unit.nva.cognito.NetworkingUtils.AUTHORIZATION_HEADER;
import static no.unit.nva.cognito.NetworkingUtils.BACKEND_CLIENT_ID;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;

public class IdentityServiceEntryUpdateHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    private final CognitoIdentityProviderClient cognitoClient;
    private final HttpClient httpClient;
    private final URI cognitoUri;

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
        String jwtToken = fetchJwtToken();
        context.getLogger().log(jwtToken);
        return input;
    }

    private String fetchJwtToken() {
        HttpRequest postRequest = formatRequestForJwtToken();
        var jwtToken = extractJwtTokenFromResponse(postRequest);
        return jwtToken;
    }

    private String extractJwtTokenFromResponse(HttpRequest postRequest) {
        return attempt(() -> sendRequest(postRequest))
            .map(HttpResponse::body)
            .map(objectMapper::mapFrom)
            .map(json -> json.get(JWT_TOKEN_FIELD))
            .map(Objects::toString)
            .orElseThrow();
    }

    private HttpRequest formatRequestForJwtToken() {
        return HttpRequest.newBuilder()
            .uri(cognitoUri)
            .setHeader(AUTHORIZATION_HEADER, authenticationString())
            .setHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
            .POST(clientCredentialsAuthType())
            .build();
    }

    private HttpResponse<String> sendRequest(HttpRequest postRequest) throws IOException, InterruptedException {
        return httpClient.send(postRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String authenticationString() {
        var clientSecret = fetchUserPoolClientSecret();
        return formatBasicAuthenticationHeader(clientSecret);
    }

    private String fetchUserPoolClientSecret() {
        return cognitoClient
            .describeUserPoolClient(DescribeUserPoolClientRequest.builder().clientId(BACKEND_CLIENT_ID).build())
            .userPoolClient()
            .clientSecret();
    }
}
