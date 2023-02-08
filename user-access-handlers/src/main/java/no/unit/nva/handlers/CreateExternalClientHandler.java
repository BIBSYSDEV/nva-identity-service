
package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.database.ExternalClientService;
import no.unit.nva.useraccessservice.model.CreateExternalClientResponse;
import no.unit.nva.useraccessservice.model.CreateExternalClientRequest;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;

public class CreateExternalClientHandler extends HandlerWithEventualConsistency<CreateExternalClientRequest, CreateExternalClientResponse> {

    private ExternalClientService externalClientService;

    @JacocoGenerated
    public CreateExternalClientHandler() {
        this(ExternalClientService.defaultExternalClientService());
    }

    public CreateExternalClientHandler(ExternalClientService externalClientService) {
        super(CreateExternalClientRequest.class);
        this.externalClientService = externalClientService;
    }

    @Override
    protected CreateExternalClientResponse processInput(CreateExternalClientRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        authorize(requestInfo);

        return externalClientService.createNewExternalClient(input.getClientName());
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
