package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonPointer;
import no.unit.nva.handlers.models.ImpersonationRequest;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import java.net.HttpURLConnection;
import java.util.List;

import static nva.commons.apigateway.AccessRight.ACT_AS;

public class SetImpersonationHandler extends HandlerWithEventualConsistency<ImpersonationRequest, Void> {

    public static final String IMPERSONATION = "custom:impersonating";
    public static final Environment ENVIRONMENT = new Environment();
    public static final Region AWS_REGION = Region.of(ENVIRONMENT.readEnv("AWS_REGION"));
    public static final String USER_POOL_ID = ENVIRONMENT.readEnv("USER_POOL_ID");
    private static final String CLAIMS_PATH = "/authorizer/claims/";
    private static final String USERNAME = "username";
    private static final JsonPointer USERNAME_POINTER = JsonPointer.compile(CLAIMS_PATH + USERNAME);
    private static final JsonPointer IMPERSONATION_POINTER = JsonPointer.compile(CLAIMS_PATH + IMPERSONATION);
    private static final Logger LOGGER = LoggerFactory.getLogger(SetImpersonationHandler.class);
    private final CognitoIdentityProviderClient cognitoClient;

    @JacocoGenerated
    public SetImpersonationHandler() {
        this(defaultCognitoClient());
    }

    public SetImpersonationHandler(CognitoIdentityProviderClient cognitoClient) {
        super(ImpersonationRequest.class);
        this.cognitoClient = cognitoClient;
    }

    @JacocoGenerated
    private static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(AWS_REGION)
            .build();
    }

    @Override
    protected void validateRequest(ImpersonationRequest impersonationRequest, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        authorize(requestInfo);
    }

    @Override
    protected Void processInput(ImpersonationRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {


        var nin = input.getNin();
        var username = requestInfo.getRequestContextParameterOpt(USERNAME_POINTER).orElseThrow();
        var attributes = List.of(
            AttributeType.builder().name(IMPERSONATION).value(nin).build()
        );

        LOGGER.info(String.format("User %s set impersonation as %s", requestInfo.getUserName(), nin));
        var request = AdminUpdateUserAttributesRequest.builder()
            .userPoolId(USER_POOL_ID)
            .username(username)
            .userAttributes(attributes)
            .build();

        this.cognitoClient.adminUpdateUserAttributes(request);
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(ImpersonationRequest input, Void output) {
        return HttpURLConnection.HTTP_OK;
    }

    private void authorize(RequestInfo requestInfo) throws ForbiddenException {
        if (!requestInfo.userIsAuthorized(ACT_AS) || userIsAlreadyImpersonating(requestInfo)) {
            throw new ForbiddenException();
        }
    }

    private boolean userIsAlreadyImpersonating(RequestInfo requestInfo) {
        return requestInfo.getRequestContextParameterOpt(IMPERSONATION_POINTER).isPresent();
    }
}
