
package no.unit.nva.handlers;

import static java.util.stream.Collectors.joining;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import no.unit.nva.CognitoService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.CreateExternalClientRequest;
import no.unit.nva.useraccessservice.model.CreateExternalClientResponse;
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
    private IdentityService databaseService;

    @JacocoGenerated
    public CreateExternalClientHandler() {
        this(
            IdentityService.defaultIdentityService(),
            CognitoService.defaultCognitoService()
        );
    }

    public CreateExternalClientHandler(IdentityService databaseService, CognitoService cognitoService) {
        super(CreateExternalClientRequest.class);
        this.databaseService = databaseService;
        this.cognitoService = cognitoService;
    }

    @Override
    protected CreateExternalClientResponse processInput(CreateExternalClientRequest input, RequestInfo requestInfo,
                                                        Context context)
        throws ApiGatewayException {

        authorize(requestInfo);

        validateRequest(input);

        var cognitoResponse = this.cognitoService.createUserPoolClient(input.getClientName(), input.getScopes());
        var clientDto =
            ClientDto.newBuilder()
                .withClientId(cognitoResponse.userPoolClient().clientId())
                .withCustomer(input.getCustomer())
                .build();

        databaseService.addExternalClient(clientDto);

        return formatResponse(input.getCustomer(), cognitoResponse.userPoolClient());
    }

    private void validateRequest(CreateExternalClientRequest input) throws BadRequestException {
        var issues = new ArrayList<String>();
        if (input.getScopes() == null) {
            issues.add(MISSING_SCOPES);
        }
        if (input.getCustomer() == null) {
            issues.add(MISSING_CUSTOMER);
        }
        if (input.getClientName() == null) {
            issues.add(MISSING_CLIENT_NAME);
        }
        if (!issues.isEmpty()) {
            throw new BadRequestException(
                "Issues validating request: " + issues.stream().collect(joining(", "))
            );
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
