package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import java.net.HttpURLConnection;
import java.util.List;

import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;

public class CustomerSelectionHandler extends CognitoCommunicationHandler<CustomerSelection, Void> {

    private final CognitoIdentityProviderClient cognito;
    private final String userPoolId;
    public static final String USER_POOL_ID_ENV = "USER_POOL_ID";

    @JacocoGenerated
    public CustomerSelectionHandler() {
        this(defaultCognitoClient(), new Environment());
    }

    public CustomerSelectionHandler(CognitoIdentityProviderClient cognito, Environment environment) {
        super(CustomerSelection.class, environment);
        this.cognito = cognito;
        this.userPoolId = environment.readEnv(USER_POOL_ID_ENV);
    }

    @Override
    protected void validateRequest(CustomerSelection customerSelection, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Void processInput(CustomerSelection input, RequestInfo requestInfo, Context context)
        throws ForbiddenException, UnauthorizedException {

        if (!requestInfo.getAllowedCustomers().contains(input.getCustomerId())) {
            throw new ForbiddenException();
        }

        var userAttributes = List.of(
            AttributeType.builder()
                .name(CURRENT_CUSTOMER_CLAIM)
                .value(input.getCustomerId().toString())
                .build()
        );

        cognito.adminUpdateUserAttributes(
            AdminUpdateUserAttributesRequest.builder()
                .userPoolId(userPoolId)
                .username(requestInfo.getCognitoUsername())
                .userAttributes(userAttributes)
                .build()
        );

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CustomerSelection body, Void output) {
        return HttpURLConnection.HTTP_OK;
    }
}
