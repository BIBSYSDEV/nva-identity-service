package no.unit.nva.cognito;

import static no.unit.nva.cognito.ClientPreTokenGenerationHandler.ACTING_USER_PROPERTY;
import static no.unit.nva.cognito.ClientPreTokenGenerationHandler.CLIENT_NOT_FOUND_MESSAGE;
import static no.unit.nva.cognito.ClientPreTokenGenerationHandler.CRISTIN_ORG_URI_PROPERTY;
import static no.unit.nva.cognito.ClientPreTokenGenerationHandler.CUSTOMER_PROPERTY;
import static no.unit.nva.cognito.ClientPreTokenGenerationHandler.MISSING_CLIENT_ID_MESSAGE;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.Request;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessservice.model.ClientDto;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ClientPreTokenGenerationHandlerTest {

  private static final String TOKEN_GENERATION_CLIENT_CREDENTIALS =
      "TokenGeneration_ClientCredentials";
  private static final String BLANK_CLIENT_ID = " ";
  private static final String BLANK_ACTING_USER = " ";
  private static final String[] REQUESTED_SCOPES = {
    "https://api.nva.unit.no/scopes/third-party/publication-read"
  };

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
  void shouldEnrichAccessTokenWithCustomerId() {
    var client = persistClient();
    var event = clientCredentialsEvent(client.getClientId());

    var response = handler.handleRequest(event, context);

    assertThat(
        extractAccessTokenClaims(response),
        hasEntry(CURRENT_CUSTOMER_CLAIM, client.getCustomer().toString()));
  }

  @Test
  void shouldEnrichAccessTokenWithCristinId() {
    var client = persistClient();
    var event = clientCredentialsEvent(client.getClientId());

    var response = handler.handleRequest(event, context);

    assertThat(
        extractAccessTokenClaims(response),
        hasEntry(TOP_ORG_CRISTIN_ID, client.getCristinOrgUri().toString()));
  }

  @Test
  void shouldEnrichAccessTokenWithUsername() {
    var client = persistClient();
    var event = clientCredentialsEvent(client.getClientId());

    var response = handler.handleRequest(event, context);

    assertThat(
        extractAccessTokenClaims(response), hasEntry(NVA_USERNAME_CLAIM, client.getActingUser()));
  }

  @ParameterizedTest(name = "Missing property: {0}")
  @MethodSource("clientsMissingRequiredProperty")
  void shouldThrowRuntimeExceptionWhenStoredClientIsMissingRequiredProperty(
      String missingProperty, ClientDto client) {
    identityService.addExternalClient(client);
    var event = clientCredentialsEvent(client.getClientId());

    Assertions.assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining(client.getClientId())
        .hasMessageContaining(missingProperty);
  }

  @Test
  void shouldReportEveryMissingPropertyWhenStoredClientHasNone() {
    var client = ClientDto.newBuilder().withClientId(randomString()).build();
    identityService.addExternalClient(client);
    var event = clientCredentialsEvent(client.getClientId());

    Assertions.assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining(CUSTOMER_PROPERTY)
        .hasMessageContaining(CRISTIN_ORG_URI_PROPERTY)
        .hasMessageContaining(ACTING_USER_PROPERTY);
  }

  @Test
  void shouldThrowRuntimeExceptionWhenClientWithIdDoesNotExist() {
    var unknownClientId = randomString();
    var event = clientCredentialsEvent(unknownClientId);

    Assertions.assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(RuntimeException.class)
        .hasMessage(CLIENT_NOT_FOUND_MESSAGE.formatted(unknownClientId))
        .hasCauseInstanceOf(NotFoundException.class);
  }

  @ParameterizedTest(name = "Client id: \"{0}\"")
  @NullAndEmptySource
  @ValueSource(strings = {BLANK_CLIENT_ID})
  void shouldThrowRuntimeExceptionWhenClientIdIsMissing(String clientId) {
    var event = clientCredentialsEvent(clientId);

    Assertions.assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(RuntimeException.class)
        .hasMessage(MISSING_CLIENT_ID_MESSAGE);
  }

  @Test
  void shouldNotOverwriteIncomingEventApartFromResponse() {
    var client = persistClient();
    var event = clientCredentialsEvent(client.getClientId());

    var response = handler.handleRequest(event, context);

    assertThat(response.getTriggerSource(), is(equalTo(TOKEN_GENERATION_CLIENT_CREDENTIALS)));
    assertThat(response.getRequest().getScopes(), is(equalTo(REQUESTED_SCOPES)));
  }

  private static Stream<Arguments> clientsMissingRequiredProperty() {
    return Stream.of(
        Arguments.of(
                CUSTOMER_PROPERTY,
                ClientDto.newBuilder()
                    .withClientId(randomString())
                    .withCristinOrgUri(randomUri())
                    .withActingUser(randomString())
                    .build()),
            Arguments.of(
                CRISTIN_ORG_URI_PROPERTY,
                ClientDto.newBuilder()
                    .withClientId(randomString())
                    .withCustomer(randomUri())
                    .withActingUser(randomString())
                    .build()),
        Arguments.of(
                ACTING_USER_PROPERTY,
                ClientDto.newBuilder()
                    .withClientId(randomString())
                    .withCustomer(randomUri())
                    .withCristinOrgUri(randomUri())
                    .build()),
            Arguments.of(
                ACTING_USER_PROPERTY,
                ClientDto.newBuilder()
                    .withClientId(randomString())
                    .withCustomer(randomUri())
                    .withCristinOrgUri(randomUri())
                    .withActingUser(BLANK_ACTING_USER)
                    .build()));
  }

  private static CognitoUserPoolPreTokenGenerationEventV2 clientCredentialsEvent(String clientId) {
    var event = new CognitoUserPoolPreTokenGenerationEventV2();
    event.setTriggerSource(TOKEN_GENERATION_CLIENT_CREDENTIALS);
    event.setRequest(Request.builder().withScopes(REQUESTED_SCOPES).build());
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
