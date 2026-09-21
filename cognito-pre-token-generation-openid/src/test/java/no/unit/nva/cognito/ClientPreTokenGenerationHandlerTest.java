package no.unit.nva.cognito;

import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.M2MTokenEnricher.CLIENT_NOT_FOUND_MESSAGE;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.TRIGGER_SOURCE_CLIENT_CREDENTIALS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2;
import java.util.Map;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessservice.model.ClientDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientPreTokenGenerationHandlerTest {

  private final Context context = new FakeContext();
  private ClientPreTokenGenerationHandler handler;
  private IdentityService identityService;
  private LocalIdentityService identityServiceDb;

  @BeforeEach
  public void init() {
    identityServiceDb = new LocalIdentityService();
    var dynamoClient = identityServiceDb.initializeTestDatabase();
    identityService = IdentityService.defaultIdentityService(dynamoClient);
    handler = new ClientPreTokenGenerationHandler(identityService);
  }

  @AfterEach
  public void tearDown() {
    identityServiceDb.closeDB();
  }

  @Test
  void shouldEnrichAccessTokenWithClaims() {
    var client = persistClient();

    var response = handler.handleRequest(clientCredentialsEvent(client.getClientId()), context);

    assertThat(
        extractAccessTokenClaims(response),
        hasEntry(CURRENT_CUSTOMER_CLAIM, client.getCustomer().toString()));
  }

  @Test
  void shouldThrowExceptionWhenAccessTokenCannotBeEnriched() {
    var unknownClientId = randomString();
    var event = clientCredentialsEvent(unknownClientId);

    Assertions.assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(RuntimeException.class)
        .hasMessage(CLIENT_NOT_FOUND_MESSAGE.formatted(unknownClientId));
  }

  private static CognitoUserPoolPreTokenGenerationEventV2 clientCredentialsEvent(String clientId) {
    var event = new CognitoUserPoolPreTokenGenerationEventV2();
    event.setTriggerSource(TRIGGER_SOURCE_CLIENT_CREDENTIALS);
    event.setCallerContext(CallerContext.builder().withClientId(clientId).build());
    return event;
  }

  private static Map<String, String> extractAccessTokenClaims(
      CognitoUserPoolPreTokenGenerationEventV2 response) {
    return response
        .getResponse()
        .getClaimsAndScopeOverrideDetails()
        .getAccessTokenGeneration()
        .getClaimsToAddOrOverride();
  }

  private ClientDto persistClient() {
    var client =
        ClientDto.newBuilder()
            .withClientId(randomString())
            .withCustomer(randomUri())
            .withCristinOrgUri(randomUri())
            .withActingUser(randomString())
            .build();
    identityService.addExternalClient(client);
    return client;
  }
}
