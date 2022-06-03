package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

public class CognitoUserInfoEndpoint extends ApiGatewayHandler<Void, Map<String, String>> {

    private final CognitoIdentityProviderClient cognito;

    @JacocoGenerated
    public CognitoUserInfoEndpoint() {
        this(defaultCognito());
    }

    public CognitoUserInfoEndpoint(CognitoIdentityProviderClient cognito) {
        super(Void.class);
        this.cognito = cognito;
    }

    @Override
    protected Map<String, String> processInput(Void input, RequestInfo requestInfo, Context context) {
        var cognitoResponse = fetchUserInfo(extractAccessToken(requestInfo));
        return cognitoResponse.userAttributes()
            .stream()
            .map(this::toMapEntry)
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Map<String, String> output) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    private static CognitoIdentityProviderClient defaultCognito() {
        return null;
    }

    private SimpleEntry<String, String> toMapEntry(AttributeType att) {
        return new SimpleEntry<>(att.name(), att.value());
    }

    private GetUserResponse fetchUserInfo(String accessToken) {
        return cognito.getUser(GetUserRequest.builder().accessToken(accessToken).build());
    }

    private String extractAccessToken(RequestInfo event) {
        var authorizationHeader = removeBearerTokenPrefix(event);
        return everythingAfterBearerTokenPrefix(authorizationHeader);
    }

    private String everythingAfterBearerTokenPrefix(List<String> authorizationHeader) {
        return String.join("", authorizationHeader.subList(1, authorizationHeader.size()));
    }

    private List<String> removeBearerTokenPrefix(RequestInfo event) {
        return Arrays.asList(event.getAuthHeader().split(" "));
    }
}
