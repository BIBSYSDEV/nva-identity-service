package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import com.amazonaws.services.lambda.runtime.Context;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

import java.net.HttpURLConnection;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Collectors;

public class CognitoUserInfoEndpoint extends CognitoCommunicationHandler<Void, Map<String, String>> {

    private final CognitoIdentityProviderClient cognito;

    @JacocoGenerated
    public CognitoUserInfoEndpoint() {
        this(defaultCognitoClient(), new Environment());
    }

    public CognitoUserInfoEndpoint(CognitoIdentityProviderClient cognitoClient, Environment environment) {
        super(Void.class, environment);
        this.cognito = cognitoClient;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Map<String, String> processInput(Void input, RequestInfo requestInfo, Context context) {
        var cognitoResponse = fetchUserInfo(extractAccessToken(requestInfo));
        var attributes = cognitoResponse.userAttributes()
                             .stream()
                             .map(this::toMapEntry)
                             .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

        removeAccessWhenNoAcceptedTerms(attributes);

        return attributes;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod") // False positive?
    private static void removeAccessWhenNoAcceptedTerms(Map<String, String> attributes) {
        var acceptedTerms = attributes.get(CognitoClaims.CUSTOMER_ACCEPTED_TERMS);
        var currentTerms = attributes.get(CognitoClaims.CURRENT_TERMS);
        if (isNull(acceptedTerms) || !acceptedTerms.equals(currentTerms)) {
            attributes.remove(CognitoClaims.ACCESS_RIGHTS_CLAIM);
            attributes.remove(CognitoClaims.ROLES_CLAIM);
            attributes.remove(CognitoClaims.ALLOWED_CUSTOMERS_CLAIM);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Map<String, String> output) {
        return HttpURLConnection.HTTP_OK;
    }

    private SimpleEntry<String, String> toMapEntry(AttributeType att) {
        return new SimpleEntry<>(att.name(), att.value());
    }

    private GetUserResponse fetchUserInfo(String accessToken) {
        return cognito.getUser(GetUserRequest.builder().accessToken(accessToken).build());
    }
}
