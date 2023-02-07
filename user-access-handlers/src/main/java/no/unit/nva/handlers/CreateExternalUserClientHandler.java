
package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.database.ExternalUserService;
import no.unit.nva.useraccessservice.model.CreateExternalUserResponse;
import no.unit.nva.useraccessservice.model.CreateExternalUserRequest;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;

public class CreateExternalUserClientHandler extends HandlerWithEventualConsistency<CreateExternalUserRequest, CreateExternalUserResponse> {

    private ExternalUserService externalUserService;

    @JacocoGenerated
    public CreateExternalUserClientHandler() {
        this(ExternalUserService.defaultExternalUserClientService());
    }

    public CreateExternalUserClientHandler(ExternalUserService externalUserService) {
        super(CreateExternalUserRequest.class);
        this.externalUserService = externalUserService;
    }

    @Override
    protected CreateExternalUserResponse processInput(CreateExternalUserRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        authorize(requestInfo);

        return externalUserService.createNewExternalUserClient(input.getClientName());
    }

    @Override
    protected Integer getSuccessStatusCode(CreateExternalUserRequest input, CreateExternalUserResponse output) {
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
