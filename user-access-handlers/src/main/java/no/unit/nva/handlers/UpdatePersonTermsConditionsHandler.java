package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonPointer;
import java.util.List;
import no.unit.nva.cognito.CognitoClaims;
import no.unit.nva.database.TermsAndConditionsService;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

public class UpdatePersonTermsConditionsHandler extends
    ApiGatewayHandler<TermsConditionsResponse, TermsConditionsResponse> {

    public static final String AWS_REGION_ENV = "AWS_REGION";
    public static final String USER_POOL_ID_ENV = "USER_POOL_ID";
    private final TermsAndConditionsService service;
    private final CognitoIdentityProviderClient cognito;
    private final String userPoolId;
    private static final JsonPointer USERNAME_POINTER = JsonPointer.compile("/authorizer/claims/username");

    @JacocoGenerated
    public UpdatePersonTermsConditionsHandler() {
        this(new TermsAndConditionsService(), defaultCognitoClient(), new Environment());
    }

    public UpdatePersonTermsConditionsHandler(TermsAndConditionsService service,
                                              CognitoIdentityProviderClient cognito,
                                              Environment environment) {
        super(TermsConditionsResponse.class);
        this.service = service;
        this.cognito = cognito;
        this.userPoolId = environment.readEnv(USER_POOL_ID_ENV);
    }


    @Override
    protected void validateRequest(
        TermsConditionsResponse input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        requestInfo.getPersonCristinId();
        requestInfo.getCurrentCustomer();
    }

    @Override
    protected TermsConditionsResponse processInput(
        TermsConditionsResponse input, RequestInfo requestInfo, Context context) throws ApiGatewayException {

        List<AttributeType> userAttributes = List.of(
            AttributeType.builder()
                .name(CognitoClaims.CUSTOMER_ACCEPTED_TERMS)
                .value(input.termsConditionsUri().toString())
                .build(),
            AttributeType.builder()
                .name(CognitoClaims.CURRENT_TERMS)
                .value(service.getCurrentTermsAndConditions().termsConditionsUri().toString())
                .build()
        );

        var username = requestInfo.getRequestContextParameterOpt(USERNAME_POINTER).orElseThrow();

        cognito.adminUpdateUserAttributes(
            AdminUpdateUserAttributesRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .userAttributes(userAttributes)
                .build()
        );

        return service.updateTermsAndConditions(
            requestInfo.getPersonCristinId(),
            input.termsConditionsUri(),
            requestInfo.getUserName()
        );
    }

    @Override
    protected Integer getSuccessStatusCode(
        TermsConditionsResponse termsConditionsResponse, TermsConditionsResponse o) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    protected static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
                   .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                   .httpClient(UrlConnectionHttpClient.create())
                   .region(Region.of(new Environment().readEnv(AWS_REGION_ENV)))
                   .build();
    }
}
