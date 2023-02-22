package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public class ExternalClientPreTokenHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalClientPreTokenHandler.class);

    @JacocoGenerated
    public ExternalClientPreTokenHandler() {
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {
        LOGGER.info("ExternalClientPreTokenHandler called");
        LOGGER.info(input.toString());
        LOGGER.info(context.toString());
        return input;
    }
}
