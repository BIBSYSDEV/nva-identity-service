package no.unit.nva.cognito;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.fasterxml.jackson.jr.ob.JSON;

public class IdentityServiceEntryUpdateHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {
        String userAttributes=
            attempt(()->JSON.std.asString(input.getRequest().getUserAttributes().entrySet()))
                .orElseThrow();
        context.getLogger().log(userAttributes);
        return input;
    }
}
