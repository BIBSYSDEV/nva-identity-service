package no.unit.nva.cognito;

import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import static no.unit.nva.database.IdentityService.defaultIdentityService;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2;
import no.unit.nva.database.IdentityService;
import nva.commons.core.JacocoGenerated;

public class ClientPreTokenGenerationHandler
    implements RequestHandler<
        CognitoUserPoolPreTokenGenerationEventV2, CognitoUserPoolPreTokenGenerationEventV2> {

  private final M2MTokenEnricher tokenEnricher;

  @JacocoGenerated
  public ClientPreTokenGenerationHandler() {
    this(defaultIdentityService(DEFAULT_DYNAMO_CLIENT));
  }

  public ClientPreTokenGenerationHandler(IdentityService identityService) {
    this.tokenEnricher = new M2MTokenEnricher(identityService);
  }

  @Override
  public CognitoUserPoolPreTokenGenerationEventV2 handleRequest(
      CognitoUserPoolPreTokenGenerationEventV2 input, Context context) {
    return tokenEnricher.enrichAccessToken(input);
  }
}
