
package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.GetExternalClientResponse;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class GetExternalClientUserinfoHandler
    extends HandlerWithEventualConsistency<Void, GetExternalClientResponse> {

    private IdentityService databaseService;

    @JacocoGenerated
    public GetExternalClientUserinfoHandler() {
        this(
            IdentityService.defaultIdentityService()
        );
    }

    public GetExternalClientUserinfoHandler(IdentityService databaseService) {
        super(Void.class);
        this.databaseService = databaseService;
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
