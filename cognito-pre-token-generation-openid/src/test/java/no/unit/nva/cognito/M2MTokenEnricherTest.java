package no.unit.nva.cognito;

import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.cognito.M2MTokenEnricher.ACTING_USER_PROPERTY;
import static no.unit.nva.cognito.M2MTokenEnricher.CLIENT_NOT_FOUND_MESSAGE;
import static no.unit.nva.cognito.M2MTokenEnricher.CRISTIN_ORG_URI_PROPERTY;
import static no.unit.nva.cognito.M2MTokenEnricher.CUSTOMER_PROPERTY;
import static no.unit.nva.cognito.M2MTokenEnricher.MISSING_CLIENT_ID_MESSAGE;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.TRIGGER_SOURCE_CLIENT_CREDENTIALS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.Request;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
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

class M2MTokenEnricherTest {

  private static final String BLANK_CLIENT_ID = " ";
  private static final String BLANK_ACTING_USER = " ";
  private static final String[] REQUESTED_SCOPES = {randomString()};

  private M2MTokenEnricher tokenEnricher;
  private IdentityService identityService;
  private LocalIdentityService identityServiceDb;

  @BeforeEach
  public void init() {
    identityService = initializeIdentityService();
    tokenEnricher = new M2MTokenEnricher(identityService);
  }

  @AfterEach
  public void tearDown() {
    identityServiceDb.closeDB();
  }

  @Test
  void shouldEnrichAccessTokenWithCustomerId() {
    var client = persistClient();

    var response = tokenEnricher.enrichAccessToken(clientCredentialsEvent(client.getClientId()));

    assertThat(
        extractAccessTokenClaims(response),
        hasEntry(CURRENT_CUSTOMER_CLAIM, client.getCustomer().toString()));
  }

  @Test
  void shouldEnrichAccessTokenWithAllowedCustomers() {
    var client = persistClient();

    var response = tokenEnricher.enrichAccessToken(clientCredentialsEvent(client.getClientId()));

    assertThat(
        extractAccessTokenClaims(response),
        hasEntry(ALLOWED_CUSTOMERS_CLAIM, client.getCustomer().toString()));
  }

  @Test
  void shouldEnrichAccessTokenWithCristinId() {
    var client = persistClient();

    var response = tokenEnricher.enrichAccessToken(clientCredentialsEvent(client.getClientId()));

    assertThat(
        extractAccessTokenClaims(response),
        hasEntry(TOP_ORG_CRISTIN_ID, client.getCristinOrgUri().toString()));
  }

  @Test
  void shouldEnrichAccessTokenWithUsername() {
    var client = persistClient();

    var response = tokenEnricher.enrichAccessToken(clientCredentialsEvent(client.getClientId()));

    assertThat(
        extractAccessTokenClaims(response), hasEntry(NVA_USERNAME_CLAIM, client.getActingUser()));
  }

  @ParameterizedTest(name = "Missing property: {0}")
  @MethodSource("clientsMissingRequiredProperty")
  void shouldThrowRuntimeExceptionWhenStoredClientIsMissingRequiredProperty(
      String missingProperty, ClientDto client) {
    identityService.addExternalClient(client);
    var event = clientCredentialsEvent(client.getClientId());

    Assertions.assertThatThrownBy(() -> tokenEnricher.enrichAccessToken(event))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining(client.getClientId())
        .hasMessageContaining(missingProperty);
  }

  @Test
  void shouldReportEveryMissingPropertyWhenStoredClientHasNone() {
    var client = ClientDto.newBuilder().withClientId(randomString()).build();
    identityService.addExternalClient(client);
    var event = clientCredentialsEvent(client.getClientId());

    Assertions.assertThatThrownBy(() -> tokenEnricher.enrichAccessToken(event))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining(CUSTOMER_PROPERTY)
        .hasMessageContaining(CRISTIN_ORG_URI_PROPERTY)
        .hasMessageContaining(ACTING_USER_PROPERTY);
  }

  @Test
  void shouldThrowRuntimeExceptionWhenClientWithIdDoesNotExist() {
    var unknownClientId = randomString();
    var event = clientCredentialsEvent(unknownClientId);

    Assertions.assertThatThrownBy(() -> tokenEnricher.enrichAccessToken(event))
        .isInstanceOf(RuntimeException.class)
        .hasMessage(CLIENT_NOT_FOUND_MESSAGE.formatted(unknownClientId))
        .hasCauseInstanceOf(NotFoundException.class);
  }

  @ParameterizedTest(name = "Client id: \"{0}\"")
  @NullAndEmptySource
  @ValueSource(strings = {BLANK_CLIENT_ID})
  void shouldThrowRuntimeExceptionWhenClientIdIsMissing(String clientId) {
    var event = clientCredentialsEvent(clientId);

    Assertions.assertThatThrownBy(() -> tokenEnricher.enrichAccessToken(event))
        .isInstanceOf(RuntimeException.class)
        .hasMessage(MISSING_CLIENT_ID_MESSAGE);
  }

  @Test
  void shouldNotOverwriteIncomingEventApartFromResponse() {
    var client = persistClient();

    var response = tokenEnricher.enrichAccessToken(clientCredentialsEvent(client.getClientId()));

    assertThat(response.getTriggerSource(), is(equalTo(TRIGGER_SOURCE_CLIENT_CREDENTIALS)));
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
    event.setTriggerSource(TRIGGER_SOURCE_CLIENT_CREDENTIALS);
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

  private IdentityService initializeIdentityService() {
    this.identityServiceDb = new LocalIdentityService();
    var client = identityServiceDb.initializeTestDatabase();
    return IdentityService.defaultIdentityService(client);
  }
}
