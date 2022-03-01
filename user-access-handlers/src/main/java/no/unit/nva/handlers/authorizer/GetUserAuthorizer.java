package no.unit.nva.handlers.authorizer;

import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import no.unit.commons.apigateway.authentication.ForbiddenException;
import no.unit.commons.apigateway.authentication.RequestAuthorizer;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class GetUserAuthorizer extends RequestAuthorizer {


    public GetUserAuthorizer(){
        super();
    }

    @Override
    protected boolean callerIsAllowedToPerformAction(APIGatewayCustomAuthorizerEvent event)
        throws ForbiddenException {
        System.out.println(event.toString());
        return true;
    }

    @Override
    protected String principalId() {
        return null;
    }

    @Override
    protected String fetchSecret() throws ForbiddenException {
        return null;
    }
}
