package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
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

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;

import static nva.commons.apigateway.AccessRight.MANAGE_EXTERNAL_CLIENTS;

public class CreateExternalClientHandler
    extends HandlerWithEventualConsistency<CreateExternalClientRequest, CreateExternalClientResponse> {

    public static final String MISSING_SCOPES = "Request does not contain 'scopes'";
    public static final String MISSING_CUSTOMER_URI = "Request does not contain 'customerUri'";
    public static final String MISSING_CRISTIN_ORG_URI = "Request does not contain 'cristinOrgUri'";
    public static final String MISSING_ACTING_USER = "Request does not contain 'actingUser'";
    public static final String MISSING_CLIENT_NAME = "Request does not contain 'clientName'";
    private static final String EXTERNAL_USER_POOL_URL = new Environment().readEnv("EXTERNAL_USER_POOL_URL");
    private final CognitoService cognitoService;
    private final IdentityService databaseService;

    @JacocoGenerated
    public CreateExternalClientHandler() {
        this(
            IdentityService.defaultIdentityService(),
            CognitoService.defaultCognitoService(),
            new Environment()
        );
    }

    public CreateExternalClientHandler(IdentityService databaseService, CognitoService cognitoService,
                                       Environment environment) {
        super(CreateExternalClientRequest.class, environment);
        this.databaseService = databaseService;
        this.cognitoService = cognitoService;
    }

    @Override
    protected void validateRequest(CreateExternalClientRequest createExternalClientRequest, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        authorize(requestInfo);
        validateRequest(createExternalClientRequest);

    }

    @Override
    protected CreateExternalClientResponse processInput(CreateExternalClientRequest input, RequestInfo requestInfo,
                                                        Context context)
        throws ApiGatewayException {

        var cognitoResponse = this.cognitoService.createUserPoolClient(input.getClientName(), input.getScopes());
        var clientDto =
            ClientDto.newBuilder()
                .withClientId(cognitoResponse.userPoolClient().clientId())
                .withCustomer(input.getCustomerUri())
                .withCristinOrgUri(input.getCristinOrgUri())
                .withActingUser(input.getActingUser())
                .build();

        databaseService.addExternalClient(clientDto);

        return formatResponse(input.getCustomerUri(), cognitoResponse.userPoolClient());
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

    private void validateRequest(CreateExternalClientRequest input) throws BadRequestException {
        var issues = new ArrayList<String>();
        if (input.getScopes() == null) {
            issues.add(MISSING_SCOPES);
        }
        if (input.getCustomerUri() == null) {
            issues.add(MISSING_CUSTOMER_URI);
        }
        if (input.getCristinOrgUri() == null) {
            issues.add(MISSING_CRISTIN_ORG_URI);
        }
        if (input.getActingUser() == null) {
            issues.add(MISSING_ACTING_USER);
        }
        if (input.getClientName() == null) {
            issues.add(MISSING_CLIENT_NAME);
        }
        if (!issues.isEmpty()) {
            throw new BadRequestException(
                "Issues validating request: " + String.join(", ", issues)
            );
        }
    }

    private void authorize(RequestInfo requestInfo) throws ForbiddenException {
        if (userIsNotAuthorized(requestInfo)) {
            throw new ForbiddenException();
        }
    }

    private boolean userIsNotAuthorized(RequestInfo requestInfo) {
        return !(requestInfo.clientIsInternalBackend()
            || requestInfo.userIsAuthorized(MANAGE_EXTERNAL_CLIENTS));
    }
}
