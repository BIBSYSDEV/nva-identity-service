package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.GetExternalClientResponse;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;

import static nva.commons.apigateway.AccessRight.MANAGE_EXTERNAL_CLIENTS;

public class GetExternalClientHandler
    extends HandlerWithEventualConsistency<Void, GetExternalClientResponse> {

    public static final String CLIENT_ID_PATH_PARAMETER_NAME = "clientId";
    private IdentityService databaseService;

    @JacocoGenerated
    public GetExternalClientHandler() {
        this(
            IdentityService.defaultIdentityService()
        );
    }

    public GetExternalClientHandler(IdentityService databaseService) {
        super(Void.class);
        this.databaseService = databaseService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        authorize(requestInfo);
    }

    @Override
    protected GetExternalClientResponse processInput(Void input, RequestInfo requestInfo,
                                                     Context context)
        throws ApiGatewayException {


        var resourceIdentifier = requestInfo.getPathParameter(CLIENT_ID_PATH_PARAMETER_NAME);

        var query = ClientDto.newBuilder().withClientId(resourceIdentifier).build();
        var result = databaseService.getClient(query);

        return formatResponse(result);
    }

    private GetExternalClientResponse formatResponse(ClientDto clientDto) {
        return new GetExternalClientResponse(
            clientDto.getClientId(),
            clientDto.getCustomer(),
            clientDto.getCristinOrgUri(),
            clientDto.getActingUser()
        );
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, GetExternalClientResponse output) {
        return HttpURLConnection.HTTP_OK;
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
