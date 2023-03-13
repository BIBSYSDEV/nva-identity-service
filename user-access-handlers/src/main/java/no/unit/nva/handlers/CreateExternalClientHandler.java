
package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import no.unit.nva.CognitoService;
import no.unit.nva.database.ExternalClientService;
import no.unit.nva.useraccessservice.model.CreateExternalClientResponse;
import no.unit.nva.useraccessservice.model.CreateExternalClientRequest;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

public class CreateExternalClientHandler
    extends HandlerWithEventualConsistency<CreateExternalClientRequest, CreateExternalClientResponse> {

    private static final String EXTERNAL_USER_POOL_URL = new Environment().readEnv("EXTERNAL_USER_POOL_URL");
    public static final String MISSING_SCOPES = "Request does not contain 'scopes'";
    public static final String MISSING_CUSTOMER = "Request does not contain 'customer'";
    public static final String MISSING_CLIENT_NAME = "Request does not contain 'clientName'";
    private CognitoService cognitoService;
    private ExternalClientService externalClientService;

    @JacocoGenerated
    public CreateExternalClientHandler() {
        this(
            ExternalClientService.defaultExternalClientService(),
            CognitoService.defaultCognitoService()
        );
    }

    public CreateExternalClientHandler(ExternalClientService externalClientService, CognitoService cognitoService) {
        super(CreateExternalClientRequest.class);
        this.externalClientService = externalClientService;
        this.cognitoService = cognitoService;
    }

    @Override
    protected CreateExternalClientResponse processInput(CreateExternalClientRequest input, RequestInfo requestInfo,
                                                        Context context)
        throws ApiGatewayException {

        authorize(requestInfo);

        validateRequest(input);

        var cognitoResponse = this.cognitoService.createUserPoolClient(input.getClientName(), input.getScopes());
        externalClientService.createNewExternalClient(
            input.getClientName(),
            input.getCustomer()
        );

        return formatResponse(input.getCustomer(), cognitoResponse.userPoolClient());
    }

    private void validateRequest(CreateExternalClientRequest input) throws BadRequestException {
        if (input.getScopes() == null) {
            throw new BadRequestException(MISSING_SCOPES);
        }
        if (input.getCustomer() == null) {
            throw new BadRequestException(MISSING_CUSTOMER);
        }
        if (input.getClientName() == null) {
            throw new BadRequestException(MISSING_CLIENT_NAME);
        }
    }

    private CreateExternalClientResponse formatResponse(
        URI customer,
        UserPoolClientType userPoolClientType) {
        return new CreateExternalClientResponse(
            userPoolClientType.clientId(),
            userPoolClientType.clientSecret(),
            EXTERNAL_USER_POOL_URL,
            customer,
            userPoolClientType.allowedOAuthScopes()
        );
    }

    @Override
    protected Integer getSuccessStatusCode(CreateExternalClientRequest input, CreateExternalClientResponse output) {
        return HttpURLConnection.HTTP_OK;
    }

    private void authorize(RequestInfo requestInfo) throws ForbiddenException {
        if (userIsNotAuthorized(requestInfo)) {
            throw new ForbiddenException();
        }
    }

    private boolean userIsNotAuthorized(RequestInfo requestInfo) {
        return !(requestInfo.clientIsInternalBackend()
                 || requestInfo.userIsApplicationAdmin());
    }
}
