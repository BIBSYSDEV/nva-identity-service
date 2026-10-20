package no.unit.nva.cognito.inbound;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.inbound.FederatedLogin.FEIDE_NIN_ATTRIBUTE;
import static no.unit.nva.cognito.inbound.FederatedLogin.FULL_NAME_ATTRIBUTE;
import static no.unit.nva.cognito.inbound.FederatedLogin.GIVEN_NAME_ATTRIBUTE;
import static no.unit.nva.cognito.inbound.FederatedLogin.NIN_CLAIM;
import static no.unit.nva.cognito.inbound.FederatedLogin.PRINCIPAL_NAME_CLAIM;
import static no.unit.nva.cognito.inbound.FederatedLogin.SURNAME_ATTRIBUTE;
import static no.unit.nva.cognito.inbound.InboundFederationHandler.LINK_MAX_AGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_NAME;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_VALUE;
import static no.unit.nva.useraccessservice.userceation.testing.cristin.RandomNin.randomNin;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDto;
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
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceAlreadyExistsException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceLoginNotCompletedException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceMissingNinException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogRecorder;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
class InboundFederationHandlerTest {

  /**
   * Plain Jackson, configured like the Lambda runtime's serializer: unknown properties are ignored
   * and nothing else is special-cased.
   */
  private static final JsonMapper RUNTIME_LIKE_MAPPER =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private static final Path EVENT_RESOURCE = Path.of("cognito", "inbound_federation_event.json");
  private static final String NOT_A_NUMBER = "123456789";
  private static final String USERNAME_PREFIX = "Dataporten_";
  private static final String EMAIL = "example@example.no";
  private static final String HOME_ORGANIZATION = "example.no";
  private static final String FIXTURE_GIVEN_NAME = "Exemplaria";
  private static final String FIXTURE_FAMILY_NAME = "Fixturesen";
  private static final String OTHER_GIVEN_NAME = "Kari";
  private static final String OTHER_FAMILY_NAME = "Nordmann";
  private static final String MISSING_NIN_CODE = "IdentityService-1003";
  private static final String LOGIN_NOT_COMPLETED_CODE = "IdentityService-1005";
  private static final String SUB = "sub";
  private static final Duration ONE_DAY = Duration.ofDays(1);
  private static final Duration ONE_HOUR = Duration.ofHours(1);
  private static final int SOME_VALUES = 5;

  private final FakeSecretsManagerClient secretsManagerClient = new FakeSecretsManagerClient();
  private final Clock clock = Clock.systemUTC();
  private Context context;
  private InboundFederationHandler handler;
  private IdentityService identityService;
  private DynamoDBCustomerService customerService;
  private LocalIdentityService identityServiceDb;
  private LocalCustomerServiceDatabase customerServiceDatabase;
  private MockPersonRegistry mockPersonRegistry;
  private AuthenticationScenarios scenarios;
  private PersonRegistry personRegistry;
  private HttpHeaders defaultRequestHeaders;
  private LogRecorder logRecorder;
  private InboundFederationEvent lastEvent;

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
    var wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
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
    logRecorder = LogRecorder.forRoot(InboundFederationHandlerTest.class);
    handler = handlerWith(personRegistry, clock);
  }

  /** Whatever path a login took, nothing the provider sent may end up in the log. */
  @AfterEach
  void tearDown() {
    if (nonNull(lastEvent)) {
      assertThat(logRecorder.asString()).doesNotContain(attributeValues(lastEvent));
    }
    customerServiceDatabase.deleteDatabase();
    identityServiceDb.closeDB();
  }

  @Test
  void shouldCreateUserEntriesAndLinkAndPersonRowsOnFirstLogin() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var cristinId = scenarios.getCristinIdForPerson(person.nin());
    var event = feideLogin(person.nin());
    var before = clock.instant();

    var response = handler.handleRequest(event, context);

    assertThat(response).isSameAs(event);
    assertThat(response.getResponse().getUserAttributesToMap()).isEmpty();
    assertThat(scanAllUsers())
        .singleElement()
        .satisfies(
            user -> {
              assertThat(user.getCristinId()).isEqualTo(cristinId);
              assertThat(user.getFeideIdentifier()).isEqualTo(EMAIL);
            });
    assertThat(identityService.fetchLink(usernameOf(event)))
        .get()
        .satisfies(
            link -> {
              assertThat(link.cristinId()).isEqualTo(cristinId);
              assertThat(link.feideIdentifier()).isEqualTo(EMAIL);
              assertThat(link.orgFeideDomain()).isEqualTo(HOME_ORGANIZATION);
              assertThat(link.modified()).isBetween(before, clock.instant());
            });
    assertThat(identityService.fetchLinkedPerson(cristinId))
        .get()
        .satisfies(
            linkedPerson -> {
              assertThat(linkedPerson.customers())
                  .isEqualTo(customerIds(scenarios.fetchCustomersForPerson(person.nin())));
              assertThat(linkedPerson.lastVerified()).isBetween(before, clock.instant());
            });
    assertThat(logRecorder.asString())
        .contains(
            "Inbound federation for user "
                + usernameOf(event)
                + " resolved to "
                + cristinId
                + " through the person registry")
        .contains(
            "Name from Dataporten for user " + usernameOf(event) + " matches the registry: false");
  }

  @Test
  void shouldAskTheRegistryAgainAndWriteTheRowsAgainWhenTheUserHasALink() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var cristinId = scenarios.getCristinIdForPerson(person.nin());
    var event = feideLogin(person.nin());
    handler.handleRequest(event, context);
    var firstLogin = identityService.fetchLink(usernameOf(event)).orElseThrow().modified();
    var anHourLater = handlerWith(personRegistry, Clock.offset(clock, ONE_HOUR));

    anHourLater.handleRequest(event, context);

    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::modified)
        .get()
        .satisfies(modified -> assertThat(modified).isAfterOrEqualTo(firstLogin.plus(ONE_HOUR)));
    assertThat(identityService.fetchLinkedPerson(cristinId))
        .map(LinkedPerson::lastVerified)
        .get()
        .satisfies(verified -> assertThat(verified).isAfter(firstLogin));
    assertThat(scanAllUsers()).hasSize(1);
  }

  @Test
  void shouldFailWithMissingNinWithoutCallingTheRegistryWhenTheProviderSendsNoNumber() {
    var event = feideLogin(scenarios.personWithExactlyOneActiveEmployment().nin());
    attributesOf(event).getIdToken().remove(NIN_CLAIM);
    attributesOf(event).getUserInfo().remove(FEIDE_NIN_ATTRIBUTE);

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceMissingNinException.class)
        .hasMessageContaining(MISSING_NIN_CODE);

    WireMock.verify(0, anyRequestedFor(anyUrl()));
    assertThat(identityService.fetchLink(usernameOf(event))).isEmpty();
    assertThat(logRecorder.asString())
        .contains(
            "No national identity number from Dataporten for user "
                + usernameOf(event)
                + ", keys in idToken [");
  }

  @Test
  void shouldReadTheNumberFromUserInfoWhenTheIdTokenHasNone() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var event = feideLogin(person.nin());
    attributesOf(event).getIdToken().remove(NIN_CLAIM);

    handler.handleRequest(event, context);

    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::cristinId)
        .contains(scenarios.getCristinIdForPerson(person.nin()));
  }

  @Test
  void shouldCreateThePersonWithTheProvidersNameWhenTheRegistryHasNone() {
    var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
    mockPersonRegistry.createPostPersonStub(person.getCristinPerson());
    var event = feideLogin(person.nin(), OTHER_GIVEN_NAME, OTHER_FAMILY_NAME);

    handler.handleRequest(event, context);

    WireMock.verify(
        postRequestedFor(urlPathEqualTo("/persons"))
            .withRequestBody(containing(OTHER_GIVEN_NAME))
            .withRequestBody(containing(OTHER_FAMILY_NAME)));
    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::cristinId)
        .map(URI::toString)
        .get()
        .satisfies(cristinId -> assertThat(cristinId).endsWith(person.cristinIdentifier()));
  }

  @Test
  void shouldFetchThePersonAgainWhenTheRegistryAnswersThatItAlreadyExists() {
    var cristinId = randomUri();
    var person =
        new Person(cristinId, randomString(), FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, List.of());
    var registry = mock(PersonRegistry.class);
    when(registry.fetchPersonByNin(any()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(person));
    when(registry.createPerson(any(), any(), any()))
        .thenThrow(new IdentityServiceAlreadyExistsException("Person already exists"));
    var event = feideLogin(randomNin());

    handlerWith(registry, clock).handleRequest(event, context);

    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::cristinId)
        .contains(cristinId);
    assertThat(logRecorder.asString())
        .contains(
            "Name from Dataporten for user " + usernameOf(event) + " matches the registry: true");
  }

  @Test
  void shouldWriteALinkWithoutFeideDetailsForAnIdPortenLogin() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var event = idPortenLogin(person.nin(), FIXTURE_GIVEN_NAME + " " + FIXTURE_FAMILY_NAME);

    handler.handleRequest(event, context);

    assertThat(identityService.fetchLink(usernameOf(event)))
        .get()
        .satisfies(
            link -> {
              assertThat(link.cristinId()).isEqualTo(scenarios.getCristinIdForPerson(person.nin()));
              assertThat(link.feideIdentifier()).isNull();
              assertThat(link.orgFeideDomain()).isNull();
            });
    assertThat(scanAllUsers()).singleElement().extracting(UserDto::getFeideIdentifier).isNull();
  }

  @Test
  void shouldFailWhenThePersonMustBeCreatedAndTheProviderSendsNoUsableName() {
    var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
    mockPersonRegistry.createPostPersonStub(person.getCristinPerson());
    var event = idPortenLogin(person.nin(), FIXTURE_GIVEN_NAME);

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class)
        .hasMessageContaining(LOGIN_NOT_COMPLETED_CODE);

    WireMock.verify(0, postRequestedFor(anyUrl()));
    assertThat(logRecorder.asString())
        .contains("No name from Dataporten for user " + usernameOf(event));
  }

  @Test
  void shouldRewriteTheLinkAndLogItWhenTheRegistryAnswersAnotherPerson() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var cristinId = scenarios.getCristinIdForPerson(person.nin());
    var event = feideLogin(person.nin());
    var previousCristinId = randomUri();
    linkPerson(event, previousCristinId, FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, clock.instant());

    handler.handleRequest(event, context);

    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::cristinId)
        .contains(cristinId);
    assertThat(logRecorder.asString())
        .contains(
            "Link rewritten for user "
                + usernameOf(event)
                + " from "
                + previousCristinId
                + " to "
                + cristinId);
  }

  @Test
  void shouldAdmitAPersonWithoutCustomersWithoutUserEntries() {
    var person = scenarios.personWithExactlyOneActiveEmploymentInNonCustomer();
    var event = feideLogin(person.nin());

    handler.handleRequest(event, context);

    assertThat(scanAllUsers()).isEmpty();
    assertThat(identityService.fetchLinkedPerson(scenarios.getCristinIdForPerson(person.nin())))
        .map(LinkedPerson::customers)
        .contains(Set.of());
    assertThat(logRecorder.asString()).contains("No customer for institution");
  }

  @Test
  void shouldAdmitOnTheLinkWhenTheRegistryIsUnavailableAndTheLinkIsFreshWithTheSameName(
      WireMockRuntimeInfo wireMockRuntimeInfo) {
    var event = feideLogin(randomNin());
    var cristinId = randomUri();
    var verified = clock.instant().minus(LINK_MAX_AGE).plus(ONE_DAY);
    linkPerson(event, cristinId, FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, verified);

    var response =
        handlerWithUnreachableRegistry(wireMockRuntimeInfo).handleRequest(event, context);

    assertThat(response.getResponse().getUserAttributesToMap()).isEmpty();
    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::modified)
        .contains(verified);
    assertThat(logRecorder.asString())
        .contains("Admitted on link for user " + usernameOf(event) + " as " + cristinId);
  }

  @Test
  void shouldNotAdmitOnTheLinkWhenTheNameDiffers(WireMockRuntimeInfo wireMockRuntimeInfo) {
    var event = feideLogin(randomNin());
    linkPerson(event, randomUri(), OTHER_GIVEN_NAME, OTHER_FAMILY_NAME, clock.instant());
    var unreachable = handlerWithUnreachableRegistry(wireMockRuntimeInfo);

    assertThatThrownBy(() -> unreachable.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class)
        .hasMessageContaining(LOGIN_NOT_COMPLETED_CODE);

    assertThat(logRecorder.asString())
        .contains(
            "User "
                + usernameOf(event)
                + " not admitted on link: name does not match the linked person");
  }

  @Test
  void shouldNotAdmitOnTheLinkWhenItIsOlderThanThirtyDays(WireMockRuntimeInfo wireMockRuntimeInfo) {
    var event = feideLogin(randomNin());
    var verified = clock.instant().minus(LINK_MAX_AGE).minus(ONE_DAY);
    linkPerson(event, randomUri(), FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, verified);
    var unreachable = handlerWithUnreachableRegistry(wireMockRuntimeInfo);

    assertThatThrownBy(() -> unreachable.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class);

    assertThat(logRecorder.asString())
        .contains("User " + usernameOf(event) + " not admitted on link: link older than 30 days");
  }

  @Test
  void shouldFailWhenTheRegistryIsUnavailableAndThereIsNoLink(
      WireMockRuntimeInfo wireMockRuntimeInfo) {
    var event = feideLogin(randomNin());
    var unreachable = handlerWithUnreachableRegistry(wireMockRuntimeInfo);

    assertThatThrownBy(() -> unreachable.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class)
        .hasMessageContaining(LOGIN_NOT_COMPLETED_CODE);

    assertThat(logRecorder.asString())
        .contains("User " + usernameOf(event) + " not admitted on link: no link");
  }

  @Test
  void shouldFailWhenTheRegistryAnswersAServerErrorAndThereIsNoLink() {
    var nin = randomNin();
    mockPersonRegistry.setupServerErrorForNin(nin);
    var event = feideLogin(nin);

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class)
        .hasMessageContaining(LOGIN_NOT_COMPLETED_CODE);
  }

  @Test
  void shouldAnswerLoginNotCompletedWhenTheRowsCannotBeWritten() {
    var failingIdentityService = spy(identityService);
    doThrow(new RuntimeException("Table unavailable")).when(failingIdentityService).saveLink(any());
    var failing =
        new InboundFederationHandler(
            customerService, failingIdentityService, personRegistry, clock);
    var event = feideLogin(scenarios.personWithExactlyOneActiveEmployment().nin());

    assertThatThrownBy(() -> failing.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class)
        .hasMessageContaining(LOGIN_NOT_COMPLETED_CODE);

    assertThat(logRecorder.asString())
        .contains("Inbound federation for user " + event.getUserName() + " failed");
  }

  @Test
  void shouldNotDoubleTheProviderPrefixWhenTheEventCarriesTheCognitoUsername() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var event = feideLogin(person.nin());
    event.setUserName(USERNAME_PREFIX + event.getUserName());

    handler.handleRequest(event, context);

    assertThat(identityService.fetchLink(event.getUserName())).isPresent();
    assertThat(identityService.fetchLink(USERNAME_PREFIX + event.getUserName())).isEmpty();
  }

  @Test
  void shouldTreatANumberThatIsNotElevenDigitsAsMissing() {
    var event = feideLogin(NOT_A_NUMBER);

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceMissingNinException.class);

    WireMock.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void shouldPrintNothingButTheUsernameForALogin() {
    var nin = randomNin();
    var event = feideLogin(nin);

    assertThat(FederatedLogin.from(event).toString())
        .contains(usernameOf(event))
        .doesNotContain(nin, FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, EMAIL);
  }

  @Test
  void shouldTreatAnExistingUserEntryAsFoundWhenTheIndexMissesIt() throws ConflictException {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var customer = scenarios.fetchCustomersForPerson(person.nin()).get(0);
    var username =
        UserEntriesCreatorForPerson.usernameFor(
            person.cristinIdentifier(), customer.getCristinId());
    identityService.addUser(
        UserDto.newBuilder()
            .withUsername(username)
            .withInstitution(customer.getId())
            .withInstitutionCristinId(customer.getCristinId())
            .withCristinId(
                randomUri()) // the index is keyed on this, so the lookup by person misses
            .withGivenName(FIXTURE_GIVEN_NAME)
            .withFamilyName(FIXTURE_FAMILY_NAME)
            .build());
    var event = feideLogin(person.nin());

    handler.handleRequest(event, context);

    assertThat(scanAllUsers()).hasSize(1);
    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::cristinId)
        .contains(scenarios.getCristinIdForPerson(person.nin()));
  }

  @Test
  void shouldAnswerLoginNotCompletedWhenACustomerLookupFails() throws NotFoundException {
    var failingCustomerService = spy(customerService);
    doThrow(new RuntimeException("Table unavailable"))
        .when(failingCustomerService)
        .getCustomerByCristinId(any());
    var failing =
        new InboundFederationHandler(
            failingCustomerService, identityService, personRegistry, clock);
    var event = feideLogin(scenarios.personWithExactlyOneActiveEmployment().nin());

    assertThatThrownBy(() -> failing.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class);

    assertThat(identityService.fetchLink(usernameOf(event))).isEmpty();
  }

  @Test
  void shouldNotAdmitOnTheLinkWhenTheRegistryKnowsNoPersonAndCreatingOneFails() {
    var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
    stubFor(
        post(urlPathEqualTo("/persons"))
            .willReturn(aResponse().withStatus(HTTP_INTERNAL_ERROR).withBody("Cristin failed")));
    var event = feideLogin(person.nin());
    linkPerson(event, randomUri(), FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, clock.instant());

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class);

    WireMock.verify(1, postRequestedFor(urlPathEqualTo("/persons")));
    assertThat(logRecorder.asString()).doesNotContain("Admitted on link");
  }

  @Test
  void shouldNotAdmitOnTheLinkWhenTheLinkIsOldEvenIfThePersonRowIsFresh(
      WireMockRuntimeInfo wireMockRuntimeInfo) {
    var event = feideLogin(randomNin());
    var staleLink = clock.instant().minus(LINK_MAX_AGE).minus(ONE_DAY);
    linkPerson(
        event, randomUri(), FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, staleLink, clock.instant());
    var unreachable = handlerWithUnreachableRegistry(wireMockRuntimeInfo);

    assertThatThrownBy(() -> unreachable.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class);

    assertThat(logRecorder.asString())
        .contains("User " + usernameOf(event) + " not admitted on link: link older than 30 days");
  }

  private InboundFederationHandler handlerWith(PersonRegistry registry, Clock handlerClock) {
    return new InboundFederationHandler(customerService, identityService, registry, handlerClock);
  }

  private InboundFederationHandler handlerWithUnreachableRegistry(
      WireMockRuntimeInfo wireMockRuntimeInfo) {
    var uriWhereCristinIsUnavailable =
        URI.create("https://localhost:" + (wireMockRuntimeInfo.getHttpsPort() - 1));
    var registry =
        CristinPersonRegistry.customPersonRegistry(
            WiremockHttpClient.create(),
            uriWhereCristinIsUnavailable,
            ServiceConstants.API_DOMAIN,
            defaultRequestHeaders,
            new SecretsReader(secretsManagerClient));
    return handlerWith(registry, clock);
  }

  private void linkPerson(
      InboundFederationEvent event,
      URI cristinId,
      String givenName,
      String familyName,
      Instant verified) {
    linkPerson(event, cristinId, givenName, familyName, verified, verified);
  }

  private void linkPerson(
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

  private InboundFederationEvent feideLogin(String nin) {
    return feideLogin(nin, FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME);
  }

  /** The fixture as Feide sends it, with this person's number and name. */
  private InboundFederationEvent feideLogin(String nin, String givenName, String familyName) {
    var event = eventFromFixture();
    event.setUserName(randomString());
    attributesOf(event).getIdToken().put(NIN_CLAIM, nin);
    attributesOf(event).getUserInfo().put(FEIDE_NIN_ATTRIBUTE, nin);
    attributesOf(event).getUserInfo().put(GIVEN_NAME_ATTRIBUTE, givenName);
    attributesOf(event).getUserInfo().put(SURNAME_ATTRIBUTE, familyName);
    return remember(event);
  }

  /**
   * ID-porten through Dataporten: empty user info, the number and the full name in the id token.
   */
  private InboundFederationEvent idPortenLogin(String nin, String fullName) {
    var event = eventFromFixture();
    event.setUserName(randomString());
    attributesOf(event).getUserInfo().clear();
    attributesOf(event).getIdToken().remove(PRINCIPAL_NAME_CLAIM);
    attributesOf(event).getIdToken().put(NIN_CLAIM, nin);
    attributesOf(event).getIdToken().put(FULL_NAME_ATTRIBUTE, fullName);
    return remember(event);
  }

  private InboundFederationEvent remember(InboundFederationEvent event) {
    lastEvent = event;
    return event;
  }

  private static InboundFederationEvent eventFromFixture() {
    try {
      return RUNTIME_LIKE_MAPPER.readValue(
          IoUtils.stringFromResources(EVENT_RESOURCE), InboundFederationEvent.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private static InboundFederationEvent.Attributes attributesOf(InboundFederationEvent event) {
    return event.getRequest().getAttributes();
  }

  private static String usernameOf(InboundFederationEvent event) {
    return USERNAME_PREFIX + event.getUserName();
  }

  private static Set<URI> customerIds(List<CustomerDto> customers) {
    return customers.stream().map(CustomerDto::getId).collect(Collectors.toSet());
  }

  private List<UserDto> scanAllUsers() {
    var request = new ScanDatabaseRequestV2(randomString(), 100, null);
    return identityService.fetchOnePageOfUsers(request).getRetrievedUsers();
  }

  /**
   * Every non-blank string and every number the provider sent, except the subject, which is part of
   * the username and is meant to be logged.
   */
  private static String[] attributeValues(InboundFederationEvent event) {
    var attributes = attributesOf(event);
    var values = new ArrayList<String>();
    Stream.of(attributes.getTokenResponse(), attributes.getIdToken(), attributes.getUserInfo())
        .forEach(map -> collectScalars(map, values));
    assertThat(values).hasSizeGreaterThan(SOME_VALUES);
    return values.toArray(String[]::new);
  }

  private static void collectScalars(Map<String, Object> attributes, List<String> values) {
    attributes.forEach(
        (key, value) -> {
          if (!SUB.equals(key) && isNumberOrNonBlankText(value)) {
            values.add(String.valueOf(value));
          }
        });
  }

  private static boolean isNumberOrNonBlankText(Object value) {
    return value instanceof Number || (value instanceof String text && !text.isBlank());
  }
}
