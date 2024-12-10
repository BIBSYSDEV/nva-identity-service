package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.ArrayList;
import java.util.Arrays;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

public class UpdatePersonTermsConditionsHandler extends
    ApiGatewayHandler<TermsConditionsResponse, TermsConditionsResponse> {

    public static final String AWS_REGION_ENV = "AWS_REGION";
    public static final String USER_POOL_ID_ENV = "USER_POOL_ID";
    private final TermsAndConditionsService service;
    private final CognitoIdentityProviderClient cognito;
    public static final String SINGLE_SPACE = " ";
    private final String userPoolId;

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
        var accessToken = extractAccessToken(requestInfo);
        var userAttributes = fetchUserInfo(accessToken).userAttributes();

        userAttributes = updateOrAddUserAttribute(userAttributes, CognitoClaims.CUSTOMER_ACCEPTED_TERMS,
                                                   input.termsConditionsUri().toString());
        userAttributes = updateOrAddUserAttribute(userAttributes, CognitoClaims.CURRENT_TERMS,
                                                  service.getCurrentTermsAndConditions().toString());

        cognito.adminUpdateUserAttributes(
            AdminUpdateUserAttributesRequest.builder()
                .userPoolId(userPoolId)
                .username(requestInfo.getUserName())
                .userAttributes(userAttributes)
                .build()
        );

        return service.updateTermsAndConditions(
            requestInfo.getPersonCristinId(),
            input.termsConditionsUri(),
            requestInfo.getUserName()
        );
    }

    private List<AttributeType> updateOrAddUserAttribute(List<AttributeType> userAttributes, String attributeName, String attributeValue) {
        // Filter out the existing attribute with the same name (if it exists)
        var updatedAttributes = new ArrayList<>(userAttributes.stream()
                                                                    .filter(attribute -> !attribute.name()
                                                                                              .equals(attributeName))
                                                                    .toList());

        // Add the new attribute
        updatedAttributes.add(AttributeType.builder().name(attributeName).value(attributeValue).build());

        return updatedAttributes;
    }

    private GetUserResponse fetchUserInfo(String accessToken) {
        return cognito.getUser(GetUserRequest.builder().accessToken(accessToken).build());
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

    protected String extractAccessToken(RequestInfo event) {
        var authorizationHeader = removeBearerTokenPrefix(event);
        return everythingAfterBearerTokenPrefix(authorizationHeader);
    }

    private String everythingAfterBearerTokenPrefix(List<String> authorizationHeader) {
        return String.join(EMPTY_STRING, authorizationHeader.subList(1, authorizationHeader.size()));
    }

    private List<String> removeBearerTokenPrefix(RequestInfo event) {
        return Arrays.asList(event.getAuthHeader().split(SINGLE_SPACE));
    }
}
