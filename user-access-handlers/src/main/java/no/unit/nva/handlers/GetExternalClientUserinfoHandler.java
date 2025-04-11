package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.GetExternalClientResponse;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;

public class GetExternalClientUserinfoHandler
    extends HandlerWithEventualConsistency<Void, GetExternalClientResponse> {

    private IdentityService databaseService;

    @JacocoGenerated
    public GetExternalClientUserinfoHandler() {
        this(
            IdentityService.defaultIdentityService(), new Environment()
        );
    }

    public GetExternalClientUserinfoHandler(IdentityService databaseService, Environment environment) {
        super(Void.class, environment);
        this.databaseService = databaseService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected GetExternalClientResponse processInput(Void input, RequestInfo requestInfo,
                                                     Context context)
        throws ApiGatewayException {

        var query = ClientDto.newBuilder().withClientId(requestInfo.getClientId().orElseThrow()).build();
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
}
