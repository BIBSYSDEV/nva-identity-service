package no.unit.nva.cognito;

import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import nva.commons.core.Environment;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;

public class IdentityServiceEntryUpdateHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    private static final String BACKEND_CLIENT_ID = new Environment().readEnv("BACKEND_CLIENT_ID");
    private final CognitoIdentityProviderClient cognitoClient;

    public IdentityServiceEntryUpdateHandler(CognitoIdentityProviderClient cognitoClient){

        this.cognitoClient = cognitoClient;
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {
        var clientSecret=cognitoClient
            .describeUserPoolClient(DescribeUserPoolClientRequest.builder().clientId(BACKEND_CLIENT_ID).build())
            .userPoolClient()
            .clientSecret();

        context.getLogger().log(clientSecret);


        String userAttributes =
            attempt(() -> objectMapper.asString(input.getRequest().getUserAttributes().entrySet()))
                .orElseThrow();
        context.getLogger().log(userAttributes);
        return input;
    }
}
