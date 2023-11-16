
package no.unit.nva.handlers;

import static nva.commons.apigateway.RequestInfoConstants.ISS;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonPointer;
import java.net.HttpURLConnection;
import java.util.List;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

public class StopImpersonationHandler extends HandlerWithEventualConsistency<Void, Void> {

    private static final String CLAIMS_PATH = "/authorizer/claims/";
    private static final String USERNAME = "username";
    private static final String IMPERSONATION = "custom:impersonating";
    private static final JsonPointer USERNAME_POINTER = JsonPointer.compile(CLAIMS_PATH + USERNAME);
    private final CognitoIdentityProviderClient cognitoClient;
    public static final Environment ENVIRONMENT = new Environment();
    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));

    @JacocoGenerated
    public StopImpersonationHandler() {
        this(defaultCognitoClient());
    }

    public StopImpersonationHandler(CognitoIdentityProviderClient cognitoClient) {
        super(Void.class);
        this.cognitoClient = cognitoClient;
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var username = requestInfo.getRequestContextParameterOpt(USERNAME_POINTER).orElseThrow();
        var userPoolId = requestInfo.getRequestContextParameterOpt(ISS).orElseThrow();
        var attributes = List.of(
            AttributeType.builder().name(IMPERSONATION).value("").build()
        );

        var request =  AdminUpdateUserAttributesRequest.builder()
                                         .userPoolId(userPoolId)
                                         .username(username)
                                         .userAttributes(attributes)
                                         .build();
        this.cognitoClient.adminUpdateUserAttributes(request);
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    private static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
                   .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                   .httpClient(UrlConnectionHttpClient.create())
                   .region(AWS_REGION)
                   .build();
    }
}
