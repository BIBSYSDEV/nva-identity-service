package no.unit.nva.cognito;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolCreateAuthChallengeEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolCreateAuthChallengeEvent.Response;
import java.util.Map;
import no.unit.nva.identityservice.json.JsonConfig;

public class CreateChallengerHandler
    implements RequestHandler<CognitoUserPoolCreateAuthChallengeEvent, CognitoUserPoolCreateAuthChallengeEvent> {

    @Override
    public CognitoUserPoolCreateAuthChallengeEvent handleRequest(CognitoUserPoolCreateAuthChallengeEvent input,
                                                                 Context context) {
        Map<String, String> userAttributes = input.getRequest().getUserAttributes();
        String json = attempt(()->JsonConfig.objectMapper.asString(userAttributes)).orElseThrow();
        context.getLogger().log(json);
        Map<String, String> publicChallengeParameters= Map.of("hello","world","orestis","gkorgkas");
        input.setResponse(Response.builder()
                              .withPublicChallengeParameters(publicChallengeParameters)
                              .withPrivateChallengeParameters(publicChallengeParameters)
                              .build()
        );
        return input;
    }
}
