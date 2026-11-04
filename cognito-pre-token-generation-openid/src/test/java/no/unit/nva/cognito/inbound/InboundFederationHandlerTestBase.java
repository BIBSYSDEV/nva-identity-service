package no.unit.nva.cognito.inbound;

import static no.unit.nva.cognito.inbound.InboundFederationEvents.EMAIL;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.HOME_ORGANIZATION;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.usernameOf;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_NAME;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_VALUE;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.userceation.testing.cristin.AuthenticationScenarios;
import no.unit.nva.useraccessservice.userceation.testing.cristin.MockPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogRecorder;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * What every test of the inbound federation handler starts from: local user and customer tables, a
 * person registry served by WireMock, a handler on top of them, and a recorder of the log, which
 * afterwards must contain nothing the provider sent. The tests are grouped by the path a login
 * takes: {@link InboundFederationThroughTheRegistryTest}, {@link InboundFederationOnTheLinkTest}
 * and {@link InboundFederationProviderAttributesTest}.
 */
@WireMockTest(httpsEnabled = true)
abstract class InboundFederationHandlerTestBase {

  static final String OTHER_GIVEN_NAME = "Kari";
  static final String OTHER_FAMILY_NAME = "Nordmann";
  static final String MISSING_NIN_CODE = "IdentityService-1003";
  static final String LOGIN_NOT_COMPLETED_CODE = "IdentityService-1005";
  static final Duration ONE_DAY = Duration.ofDays(1);

  /** The handler's link max age, pinned so that a change there is a deliberate change here too. */
  static final Duration THIRTY_DAYS = Duration.ofDays(30);

  private static final int SOME_VALUES = 5;

  final FakeSecretsManagerClient secretsManagerClient = new FakeSecretsManagerClient();
  final Clock clock = Clock.systemUTC();
  final InboundFederationEvents events = new InboundFederationEvents();
  Context context;
  InboundFederationHandler handler;
  IdentityService identityService;
  DynamoDBCustomerService customerService;
  MockPersonRegistry mockPersonRegistry;
  AuthenticationScenarios scenarios;
  PersonRegistry personRegistry;
  HttpHeaders defaultRequestHeaders;
  URI wiremockUri;
  LogRecorder logRecorder;
  private LocalIdentityService identityServiceDb;
  private LocalCustomerServiceDatabase customerServiceDatabase;

  @BeforeEach
  void init(WireMockRuntimeInfo wireMockRuntimeInfo)
      throws InvalidInputException, ConflictException {
    identityServiceDb = new LocalIdentityService();
    identityService =
        IdentityService.defaultIdentityService(identityServiceDb.initializeTestDatabase());
    customerServiceDatabase = new LocalCustomerServiceDatabase();
    customerServiceDatabase.setupDatabase();
    customerService = new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());

    var cristinUsername = randomString();
    var cristinPassword = randomString();
    secretsManagerClient.putSecret(
        CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_USERNAME_SECRET_KEY, cristinUsername);
    secretsManagerClient.putSecret(
        CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_PASSWORD_SECRET_KEY, cristinPassword);
    wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
    defaultRequestHeaders =
        new HttpHeaders().withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);
    mockPersonRegistry =
        new MockPersonRegistry(
            cristinUsername, cristinPassword, wiremockUri, defaultRequestHeaders);
    scenarios = new AuthenticationScenarios(mockPersonRegistry, customerService, identityService);
    personRegistry =
        CristinPersonRegistry.customPersonRegistry(
            WiremockHttpClient.create(),
            wiremockUri,
            ServiceConstants.API_DOMAIN,
            defaultRequestHeaders,
            new SecretsReader(secretsManagerClient));

    context = mock(Context.class);
    logRecorder = LogRecorder.forRoot(InboundFederationHandlerTestBase.class);
    handler = handlerWith(personRegistry, clock);
  }

  /** Whatever path a login took, nothing the provider sent may end up in the log. */
  @AfterEach
  void assertNothingFromTheProviderWasLogged() {
    var valuesSent = events.valuesSent();
    if (!valuesSent.isEmpty()) {
      assertThat(valuesSent).hasSizeGreaterThan(SOME_VALUES);
      assertThat(logRecorder.asString()).doesNotContain(valuesSent.toArray(String[]::new));
    }
  }

  @AfterEach
  void closeDatabases() {
    customerServiceDatabase.deleteDatabase();
    identityServiceDb.closeDB();
  }

  InboundFederationHandler handlerWith(PersonRegistry registry, Clock handlerClock) {
    return new InboundFederationHandler(customerService, identityService, registry, handlerClock);
  }

  /**
   * A link from this login's user to a person, and the person's row, both verified at that time.
   */
  void linkPerson(
      InboundFederationEvent event,
      URI cristinId,
      String givenName,
      String familyName,
      Instant verified) {
    linkPerson(event, cristinId, givenName, familyName, verified, verified);
  }

  void linkPerson(
      InboundFederationEvent event,
      URI cristinId,
      String givenName,
      String familyName,
      Instant linkModified,
      Instant personVerified) {
    identityService.saveLinkedPerson(
        new LinkedPerson(cristinId, givenName, familyName, Set.of(), personVerified));
    identityService.saveLink(
        new IdentityLink(usernameOf(event), cristinId, EMAIL, HOME_ORGANIZATION, linkModified));
  }

  /** A person id as the person registry client builds it. */
  static URI randomPersonId() {
    return new UriWrapper("https", ServiceConstants.API_DOMAIN)
        .addChild("cristin", "person", randomString())
        .getUri();
  }

  List<UserDto> scanAllUsers() {
    var request = new ScanDatabaseRequestV2(randomString(), 100, null);
    return identityService.fetchOnePageOfUsers(request).getRetrievedUsers();
  }
}
