package no.unit.nva.useraccessservice.usercreation;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_VALUE;
import static no.unit.nva.useraccessservice.userceation.testing.cristin.RandomNin.randomNin;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.userceation.testing.cristin.AuthenticationScenarios;
import no.unit.nva.useraccessservice.userceation.testing.cristin.MockPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceRedirectException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceUnavailableException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.logutils.LogRecorder;
import nva.commons.secrets.SecretsReader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@WireMockTest(httpsEnabled = true)
class CristinPersonRegistryTest {

  private static final String BOT_FILTER_BYPASS_HEADER_NAME = randomString();
  private static final Duration SHORT_REQUEST_TIMEOUT = Duration.ofMillis(100);
  private static final Duration CRISTIN_RESPONSE_DELAY = Duration.ofSeconds(1);
  private static final String MASKED_NUMBER = "***********";
  private PersonRegistry personRegistry;
  private FakeSecretsManagerClient secretsManagerClient;
  private AuthenticationScenarios scenarios;
  private LocalCustomerServiceDatabase customerServiceDatabase;
  private LocalIdentityService identityServiceDatabase;
  private IdentityService identityService;
  private DynamoDBCustomerService customerService;
  private MockPersonRegistry mockPersonRegistry;
  private String cristinUsername;
  private String cristinPassword;

  @BeforeEach
  void beforeEach(WireMockRuntimeInfo wireMockRuntimeInfo)
      throws InvalidInputException, ConflictException {
    setupCustomerAndIdentityService();
    cristinUsername = randomString();
    cristinPassword = randomString();
    secretsManagerClient = new FakeSecretsManagerClient();
    putCristinCredentials(secretsManagerClient, cristinUsername, cristinPassword);
    var apiDomain = ServiceConstants.API_DOMAIN;
    var wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
    var httpClient = WiremockHttpClient.create();
    var defaultRequestHeaders =
        new HttpHeaders().withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);
    personRegistry =
        CristinPersonRegistry.customPersonRegistry(
            httpClient,
            wiremockUri,
            apiDomain,
            defaultRequestHeaders,
            new SecretsReader(secretsManagerClient));
    this.mockPersonRegistry =
        new MockPersonRegistry(
            cristinUsername, cristinPassword, wiremockUri, defaultRequestHeaders);
    scenarios = new AuthenticationScenarios(mockPersonRegistry, customerService, identityService);
  }

  private void setupCustomerAndIdentityService() {
    customerServiceDatabase = new LocalCustomerServiceDatabase();
    customerServiceDatabase.setupDatabase();
    identityServiceDatabase = new LocalIdentityService();
    identityService = identityServiceDatabase.createDatabaseServiceUsingLocalStorage();
    customerService = new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());
  }

  @AfterEach
  void afterEach() {
    customerServiceDatabase.deleteDatabase();
    identityServiceDatabase.closeDB();
  }

  @Test
  void shouldThrowExceptionIfCristinIsUnavailable(WireMockRuntimeInfo wireMockRuntimeInfo) {
    var logRecorder = LogRecorder.forRoot(CristinPersonRegistryTest.class);
    var httpClient = WiremockHttpClient.create();
    var uriWhereCristinIsUnavailable =
        URI.create("https://localhost:" + (wireMockRuntimeInfo.getHttpsPort() - 1));

    var defaultRequestHeaders =
        new HttpHeaders().withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);
    personRegistry =
        CristinPersonRegistry.customPersonRegistry(
            httpClient,
            uriWhereCristinIsUnavailable,
            ServiceConstants.API_DOMAIN,
            defaultRequestHeaders,
            new SecretsReader(secretsManagerClient));
    var nin = NationalIdentityNumber.fromString(randomNin());
    var exception =
        assertThrows(
            IdentityServiceUnavailableException.class, () -> personRegistry.fetchPersonByNin(nin));
    assertThat(exception.getMessage(), not(containsString(nin.toString())));
    Assertions.assertThat(logRecorder.messages())
        .noneMatch(message -> message.contains(nin.toString()));
  }

  @Test
  void shouldFailFastWhenCristinDoesNotRespondWithinRequestTimeout(
      WireMockRuntimeInfo wireMockRuntimeInfo) {
    stubCristinRespondingSlowly();
    var registry = personRegistryWithShortRequestTimeout(wireMockRuntimeInfo);
    var nin = NationalIdentityNumber.fromString(randomNin());

    var start = Instant.now();
    var exception =
        assertThrows(
            IdentityServiceUnavailableException.class, () -> registry.fetchPersonByNin(nin));
    var elapsed = Duration.between(start, Instant.now());

    Assertions.assertThat(exception).hasCauseInstanceOf(HttpTimeoutException.class);
    Assertions.assertThat(elapsed).isLessThan(CRISTIN_RESPONSE_DELAY);
  }

  @Test
  void shouldLogTimeoutWithElapsedTimeAndWithoutNin(WireMockRuntimeInfo wireMockRuntimeInfo) {
    var logRecorder = LogRecorder.forRoot(CristinPersonRegistryTest.class);
    stubCristinRespondingSlowly();
    var registry = personRegistryWithShortRequestTimeout(wireMockRuntimeInfo);
    var nin = NationalIdentityNumber.fromString(randomNin());

    assertThrows(IdentityServiceUnavailableException.class, () -> registry.fetchPersonByNin(nin));

    Assertions.assertThat(logRecorder.messages())
        .anyMatch(message -> message.matches("(?s)Cristin call to .* timed out after \\d+ ms.*"))
        .noneMatch(message -> message.contains(nin.toString()));
  }

  private static void stubCristinRespondingSlowly() {
    stubFor(
        get(anyUrl())
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withFixedDelay((int) CRISTIN_RESPONSE_DELAY.toMillis())));
  }

  private PersonRegistry personRegistryWithShortRequestTimeout(
      WireMockRuntimeInfo wireMockRuntimeInfo) {
    return CristinPersonRegistry.customPersonRegistry(
        WiremockHttpClient.create(),
        URI.create(wireMockRuntimeInfo.getHttpsBaseUrl()),
        ServiceConstants.API_DOMAIN,
        new HttpHeaders().withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE),
        new SecretsReader(secretsManagerClient),
        SHORT_REQUEST_TIMEOUT);
  }

  @Test
  void fetchPersonByIdentifierShouldReturnPersonIfExisting() {
    var person = scenarios.personWithoutAffiliations();

    var fetchedPerson = personRegistry.fetchPersonByIdentifier(person.cristinIdentifier());
    assertThat(fetchedPerson.isPresent(), is(equalTo(true)));
  }

  @Test
  void createPersonTest() {
    var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
    mockPersonRegistry.createPostPersonStub(person.getCristinPerson());

    var fetchedPerson =
        personRegistry.createPerson(
            NationalIdentityNumber.fromString(person.nin()), randomString(), randomString());
    assertThat(fetchedPerson.isPresent(), is(equalTo(true)));
  }

  @Test
  void fetchPersonByIdentifierShouldReturnEmptyOptionalIfNotExist() {
    var fetchedPerson = personRegistry.fetchPersonByIdentifier(randomString());
    assertThat(fetchedPerson.isPresent(), is(equalTo(false)));
  }

  @Test
  void shouldThrowExceptionIfCristinRespondsWithUnexpectedJson() {
    var personNin = scenarios.failingPersonRegistryRequestBadJson().nin();
    var nin = NationalIdentityNumber.fromString(personNin);

    assertThrows(IdentityServiceException.class, () -> personRegistry.fetchPersonByNin(nin));
  }

  @Test
  void shouldThrowExceptionIfCristinRespondsWithNonOkStatusCode() {
    var logRecorder = LogRecorder.forRoot(CristinPersonRegistryTest.class);
    var personNin = scenarios.failingPersonRegistryRequestBadGateway().nin();
    var nin = NationalIdentityNumber.fromString(personNin);

    assertThrows(IdentityServiceException.class, () -> personRegistry.fetchPersonByNin(nin));
    Assertions.assertThat(logRecorder.messages())
        .anyMatch(message -> message.contains("national_id=" + MASKED_NUMBER))
        .noneMatch(message -> message.contains(personNin));
  }

  @Test
  void shouldMaskNationalIdentityNumberInLog() {
    var logRecorder = LogRecorder.forRoot(CristinPersonRegistryTest.class);
    var personNin = "12345678901";
    var nin = NationalIdentityNumber.fromString(personNin);

    mockPersonRegistry.setupServerErrorForNin(personNin);

    assertThrows(IdentityServiceException.class, () -> personRegistry.fetchPersonByNin(nin));
    Assertions.assertThat(logRecorder.asString())
        .contains("national_id=" + MASKED_NUMBER)
        .doesNotContain(personNin);
  }

  @Test
  void shouldExcludeAffiliationWhenInstitutionResolvesToDifferentOrganization() {
    var person = mockPersonRegistry.personWithActiveAffiliationAtRedirectingInstitution();
    var nin = NationalIdentityNumber.fromString(person.nin());

    var fetchedPerson = personRegistry.fetchPersonByNin(nin);
    assertThat(fetchedPerson.isPresent(), is(equalTo(true)));
    assertThat(fetchedPerson.get().getAffiliations(), emptyIterable());
  }

  @Test
  void shouldReturnEmptyListOfAffiliationsIfFieldIsMissingInCristinOnGetPerson() {
    var personNin = scenarios.personWithoutAffiliations().nin();
    var nin = NationalIdentityNumber.fromString(personNin);

    var person = personRegistry.fetchPersonByNin(nin);
    assertThat(person.isPresent(), is(equalTo(true)));
    assertThat(person.get().getAffiliations(), emptyIterable());
  }

  @Test
  void shouldFetchTheExistingPersonWhenCristinAnswersThatThePersonAlreadyExists() {
    var existing = scenarios.personWithExactlyOneActiveEmployment();
    mockPersonRegistry.setupCreatePersonAlreadyExistsError();

    var person =
        personRegistry.createPerson(
            NationalIdentityNumber.fromString(existing.nin()), "John", "Doe");

    Assertions.assertThat(person)
        .map(created -> created.getId())
        .contains(scenarios.getCristinIdForPerson(existing.nin()));
  }

  @Test
  void shouldReturnEmptyWhenCristinAnswers409ButHasNoPersonToFetch() {
    var nin = NationalIdentityNumber.fromString("12345678902");
    mockPersonRegistry.setupCreatePersonConflictError();

    Assertions.assertThat(personRegistry.createPerson(nin, "Jane", "Smith")).isEmpty();
  }

  @Test
  void shouldReportCristinAsUnavailableWhenTheCredentialsCannotBeRead(
      WireMockRuntimeInfo wireMockRuntimeInfo) {
    var registryWithoutCredentials =
        CristinPersonRegistry.customPersonRegistry(
            WiremockHttpClient.create(),
            URI.create(wireMockRuntimeInfo.getHttpsBaseUrl()),
            ServiceConstants.API_DOMAIN,
            new HttpHeaders()
                .withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE),
            new SecretsReader(new FakeSecretsManagerClient()));
    var nin = NationalIdentityNumber.fromString(randomNin());

    assertThrows(
        IdentityServiceUnavailableException.class,
        () -> registryWithoutCredentials.fetchPersonByNin(nin));
  }

  @Test
  void shouldReportCristinAsUnavailableWhenTheCredentialsCannotBeReadForALookupByIdentifier(
      WireMockRuntimeInfo wireMockRuntimeInfo) {
    var registryWithoutCredentials =
        registryReadingSecretsWith(
            wireMockRuntimeInfo, new SecretsReader(new FakeSecretsManagerClient()));

    assertThrows(
        IdentityServiceUnavailableException.class,
        () -> registryWithoutCredentials.fetchPersonByIdentifier(randomString()));
  }

  @Test
  void shouldReportCristinAsUnavailableWhenALookupByIdentifierTimesOut(
      WireMockRuntimeInfo wireMockRuntimeInfo) {
    stubCristinRespondingSlowly();
    var registry = personRegistryWithShortRequestTimeout(wireMockRuntimeInfo);

    assertThrows(
        IdentityServiceUnavailableException.class,
        () -> registry.fetchPersonByIdentifier(randomString()));
  }

  @Test
  void shouldReportCristinAsUnavailableWhenRejectedCredentialsCannotBeReadAgain(
      WireMockRuntimeInfo wireMockRuntimeInfo) {
    var secretsReader = spy(new SecretsReader(secretsManagerClient));
    doCallRealMethod()
        .doThrow(new IllegalStateException("The secret is gone"))
        .when(secretsReader)
        .fetchClassSecret(any(), any());
    var registry = registryReadingSecretsWith(wireMockRuntimeInfo, secretsReader);
    stubFor(
        WireMock.any(anyUrl())
            .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_UNAUTHORIZED)));

    assertThrows(
        IdentityServiceUnavailableException.class,
        () -> registry.fetchPersonByIdentifier(randomString()));
  }

  @Test
  void shouldReadTheCredentialsOnceForSeveralLookups(WireMockRuntimeInfo wireMockRuntimeInfo) {
    var secretsReader = spy(new SecretsReader(secretsManagerClient));
    var registry = registryReadingSecretsWith(wireMockRuntimeInfo, secretsReader);
    var person = scenarios.personWithExactlyOneActiveEmployment();

    registry.fetchPersonByNin(NationalIdentityNumber.fromString(person.nin()));
    registry.fetchPersonByIdentifier(person.cristinIdentifier());

    verify(secretsReader, times(1)).fetchClassSecret(any(), any());
  }

  @ParameterizedTest
  @ValueSource(ints = {HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN})
  void shouldReadTheCredentialsAgainAndRetryOnceWhenCristinRejectsThem(
      int rejectionStatus, WireMockRuntimeInfo wireMockRuntimeInfo) {
    var staleUsername = randomString();
    var stalePassword = randomString();
    var rotatingSecrets = new FakeSecretsManagerClient();
    putCristinCredentials(rotatingSecrets, staleUsername, stalePassword);
    var secretsReader = spy(new SecretsReader(rotatingSecrets));
    var registry = registryReadingSecretsWith(wireMockRuntimeInfo, secretsReader);
    putCristinCredentials(rotatingSecrets, cristinUsername, cristinPassword);
    var staleAuthorization = basicAuthorization(staleUsername, stalePassword);
    stubFor(
        WireMock.any(anyUrl())
            .withHeader(CristinPersonRegistry.AUTHORIZATION, WireMock.equalTo(staleAuthorization))
            .willReturn(aResponse().withStatus(rejectionStatus)));
    var person = scenarios.personWithExactlyOneActiveEmployment();

    var fetchedPerson = registry.fetchPersonByNin(NationalIdentityNumber.fromString(person.nin()));

    Assertions.assertThat(fetchedPerson)
        .get()
        .satisfies(found -> Assertions.assertThat(found.getAffiliations()).isNotEmpty());
    verify(secretsReader, times(2)).fetchClassSecret(any(), any());
    WireMock.verify(
        1,
        anyRequestedFor(anyUrl())
            .withHeader(CristinPersonRegistry.AUTHORIZATION, WireMock.equalTo(staleAuthorization)));
  }

  private PersonRegistry registryReadingSecretsWith(
      WireMockRuntimeInfo wireMockRuntimeInfo, SecretsReader secretsReader) {
    return CristinPersonRegistry.customPersonRegistry(
        WiremockHttpClient.create(),
        URI.create(wireMockRuntimeInfo.getHttpsBaseUrl()),
        ServiceConstants.API_DOMAIN,
        new HttpHeaders().withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE),
        secretsReader);
  }

  private static void putCristinCredentials(
      FakeSecretsManagerClient secrets, String username, String password) {
    secrets.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_USERNAME_SECRET_KEY, username);
    secrets.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_PASSWORD_SECRET_KEY, password);
  }

  private static String basicAuthorization(String username, String password) {
    var credentials = (username + ":" + password).getBytes(StandardCharsets.UTF_8);
    return "Basic " + Base64.getEncoder().encodeToString(credentials);
  }

  @Test
  void shouldNotRevealTheNumberWhenCristinRedirectsTheLookup() {
    var nin = randomNin();
    var redirectTarget = "https://elsewhere.example.org/persons/resolve?national_id=" + nin;
    stubFor(
        get(urlPathMatching("/persons/resolve"))
            .willReturn(aResponse().withStatus(302).withHeader("Location", redirectTarget)));

    var exception =
        assertThrows(
            IdentityServiceRedirectException.class,
            () -> personRegistry.fetchPersonByNin(NationalIdentityNumber.fromString(nin)));

    Assertions.assertThat(exception.getMessage()).doesNotContain(nin).contains("elsewhere");
  }

  @Test
  void shouldNotRevealTheNumberWhenCristinEchoesItInAnErrorBody() {
    var nin = randomNin();
    stubFor(
        get(urlPathMatching("/persons/resolve"))
            .willReturn(aResponse().withStatus(400).withBody("Invalid national_id " + nin)));
    var logRecorder = LogRecorder.forRoot(CristinPersonRegistryTest.class);

    var exception =
        assertThrows(
            IdentityServiceException.class,
            () -> personRegistry.fetchPersonByNin(NationalIdentityNumber.fromString(nin)));

    Assertions.assertThat(exception.getMessage()).doesNotContain(nin);
    Assertions.assertThat(logRecorder.asString()).doesNotContain(nin);
  }

  @Test
  void shouldMaskTheLookupParameterEvenWhenItIsNotAWellFormedNumber() {
    var notANumber = "4242424242";
    mockPersonRegistry.setupServerErrorForNin(notANumber);
    var logRecorder = LogRecorder.forRoot(CristinPersonRegistryTest.class);

    var exception =
        assertThrows(
            IdentityServiceException.class,
            () -> personRegistry.fetchPersonByNin(NationalIdentityNumber.fromString(notANumber)));

    Assertions.assertThat(exception.getMessage())
        .doesNotContain(notANumber)
        .contains("national_id=" + MASKED_NUMBER);
    Assertions.assertThat(logRecorder.asString()).doesNotContain(notANumber);
  }
}
