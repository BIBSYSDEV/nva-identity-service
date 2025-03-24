package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;

public class CustomerSelectionHandler extends CognitoCommunicationHandler<CustomerSelection, Void> {

    private final CognitoIdentityProviderClient cognito;

    @JacocoGenerated
    public CustomerSelectionHandler() {
        this(defaultCognitoClient());
    }

    public CustomerSelectionHandler(CognitoIdentityProviderClient cognito) {
        super(CustomerSelection.class);
        this.cognito = cognito;
    }

    @Override
    protected void validateRequest(CustomerSelection customerSelection, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validateInput(requestInfo.getAllowedCustomers(), customerSelection.getCustomerId());
    }

    @Override
    protected Void processInput(CustomerSelection input, RequestInfo event, Context context)
        throws UnauthorizedException {
        String cognitoGroupId = UriWrapper.fromUri(event.getIssuer()).getLastPathElement();
        updateCognitoUserEntryAttributes(input, event.getCognitoUsername(), cognitoGroupId);
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CustomerSelection body, Void output) {
        return HttpURLConnection.HTTP_OK;
    }

    private void updateCognitoUserEntryAttributes(CustomerSelection customerSelection,
                                                  String cognitoUsername,
                                                  String userPoolId) {
        var selectedCustomerCustomClaim = createAttribute(CURRENT_CUSTOMER_CLAIM, customerSelection.getCustomerId());

        var user = cognito.adminGetUser(request -> request.userPoolId(userPoolId).username(cognitoUsername));

        // Delete all existing data so there is no way to have illegal combinations of data
        cognito.adminDeleteUserAttributes(request -> request.userPoolId(userPoolId)
                                                         .username(cognitoUsername)
                                                         .userAttributeNames(getAllAttributeNames(user)));

        // Set customer selection
        cognito.adminUpdateUserAttributes(request -> request.username(cognitoUsername)
                                                         .userPoolId(userPoolId)
                                                         .userAttributes(selectedCustomerCustomClaim));
    }

    private static String[] getAllAttributeNames(AdminGetUserResponse user) {
        return user.userAttributes().stream()
                   .map(AttributeType::name)
                   .toArray(String[]::new);
    }

    private AttributeType createAttribute(String attributeName, URI attributeValue) {
        return AttributeType.builder().name(attributeName)
                   .value(attributeValue.toString())
                   .build();
    }

    private void validateInput(List<URI> allowedCustomers, URI desiredCustomerId) throws ForbiddenException {
        if (desiredCustomerIsNotAllowed(allowedCustomers, desiredCustomerId)) {
            throw new ForbiddenException();
        }
    }

    private boolean desiredCustomerIsNotAllowed(List<URI> allowedCustomers, URI desiredCustomerId) {
        return !allowedCustomers.contains(desiredCustomerId);
    }
}
