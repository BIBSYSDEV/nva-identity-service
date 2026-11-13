package no.unit.nva.cognito.inbound;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.FIXTURE_FAMILY_NAME;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.FIXTURE_GIVEN_NAME;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.usernameOf;
import static no.unit.nva.useraccessservice.userceation.testing.cristin.RandomNin.randomNin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import java.time.Duration;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceLoginNotCompletedException;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.Test;

/**
 * Logins where the person registry cannot be queried: a Cognito user it confirmed within the last
 * thirty days is admitted on the stored link when the name matches, and nothing else admits anyone.
 * A registry that answers, however oddly, never leads to the link.
 */
class InboundFederationOnTheLinkTest extends InboundFederationHandlerTestBase {

  private static final Duration SHORT_REQUEST_TIMEOUT = Duration.ofMillis(100);
  private static final Duration SLOW_ANSWER = Duration.ofSeconds(1);

  @Test
  void shouldAdmitOnTheLinkWhenTheRegistryIsUnavailableAndTheLinkIsFreshWithTheSameName() {
    registryIsDown();
    var event = events.feideLogin(randomNin());
    var cristinId = randomPersonId();
    var verified = clock.instant().minus(THIRTY_DAYS).plus(ONE_DAY);
    linkPerson(event, cristinId, FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, verified);

    var response = handler.handleRequest(event, context);

    assertThat(response.getResponse().getUserAttributesToMap()).isEmpty();
    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::modified)
        .contains(verified);
    assertThat(logRecorder.asString())
        .contains("Admitted on link for user " + usernameOf(event) + " as " + cristinId);
  }

  @Test
  void shouldAdmitOnTheLinkWhenTheRegistryAnswersAServerError() {
    var nin = randomNin();
    mockPersonRegistry.setupServerErrorForNin(nin);
    var event = events.feideLogin(nin);
    linkPerson(event, randomPersonId(), FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, clock.instant());

    handler.handleRequest(event, context);

    assertThat(logRecorder.asString()).contains("Admitted on link for user " + usernameOf(event));
  }

  @Test
  void shouldAdmitOnTheLinkWhenTheRegistryAnswersTooSlowly() {
    stubFor(
        get(anyUrl())
            .willReturn(aResponse().withStatus(200).withFixedDelay((int) SLOW_ANSWER.toMillis())));
    var event = events.feideLogin(randomNin());
    var cristinId = randomPersonId();
    linkPerson(event, cristinId, FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, clock.instant());
    var impatient = handlerWith(registryWithRequestTimeout(SHORT_REQUEST_TIMEOUT), clock);

    impatient.handleRequest(event, context);

    assertThat(logRecorder.asString())
        .containsPattern("Cristin call to .* timed out after \\d+ ms")
        .contains("Admitted on link for user " + usernameOf(event) + " as " + cristinId);
  }

  @Test
  void shouldNotAdmitOnTheLinkWhenTheNameDiffers() {
    registryIsDown();
    var event = events.feideLogin(randomNin());
    linkPerson(event, randomPersonId(), OTHER_GIVEN_NAME, OTHER_FAMILY_NAME, clock.instant());

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class)
        .hasMessageContaining(LOGIN_NOT_COMPLETED_CODE);

    assertThat(logRecorder.asString())
        .contains(
            "User "
                + usernameOf(event)
                + " not admitted on link: name does not match the linked person");
  }

  /** The age is the link's; the person row is shared by all the person's users. */
  @Test
  void shouldNotAdmitOnTheLinkWhenItIsOlderThanTheLinkMaxAgeEvenIfThePersonRowIsFresh() {
    registryIsDown();
    var event = events.feideLogin(randomNin());
    var staleLink = clock.instant().minus(THIRTY_DAYS).minus(ONE_DAY);
    linkPerson(
        event,
        randomPersonId(),
        FIXTURE_GIVEN_NAME,
        FIXTURE_FAMILY_NAME,
        staleLink,
        clock.instant());

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class);

    assertThat(logRecorder.asString())
        .contains("User " + usernameOf(event) + " not admitted on link: link older than 30 days");
  }

  @Test
  void shouldNotAdmitOnALinkWithoutATimeOfWriting() {
    registryIsDown();
    var event = events.feideLogin(randomNin());
    linkPerson(
        event, randomPersonId(), FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, null, clock.instant());

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class);

    assertThat(logRecorder.asString())
        .contains("User " + usernameOf(event) + " not admitted on link: link older than 30 days");
  }

  @Test
  void shouldNotAdmitOnALinkWhosePersonRowIsMissing() {
    registryIsDown();
    var event = events.feideLogin(randomNin());
    identityService.saveLink(
        new IdentityLink(usernameOf(event), randomPersonId(), null, null, clock.instant()));

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class);

    assertThat(logRecorder.asString())
        .contains("User " + usernameOf(event) + " not admitted on link: no link");
  }

  @Test
  void shouldFailWhenTheRegistryIsUnavailableAndThereIsNoLink() {
    registryIsDown();
    var event = events.feideLogin(randomNin());

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class)
        .hasMessageContaining(LOGIN_NOT_COMPLETED_CODE);

    assertThat(logRecorder.asString())
        .contains("User " + usernameOf(event) + " not admitted on link: no link");
  }

  @Test
  void shouldNotAdmitOnTheLinkWhenTheRegistryKnowsNoPersonAndCreatingOneFails() {
    var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
    stubFor(
        post(urlPathEqualTo("/persons"))
            .willReturn(aResponse().withStatus(HTTP_INTERNAL_ERROR).withBody("Cristin failed")));
    var event = events.feideLogin(person.nin());
    linkPerson(event, randomPersonId(), FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, clock.instant());

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class);

    WireMock.verify(1, postRequestedFor(urlPathEqualTo("/persons")));
    assertThat(logRecorder.asString()).doesNotContain("Admitted on link");
  }

  @Test
  void shouldNotAdmitOnTheLinkWhenTheRegistryRejectsTheLookup() {
    stubFor(get(urlPathMatching("/persons/resolve")).willReturn(aResponse().withStatus(400)));
    var event = events.feideLogin(randomNin());
    linkPerson(event, randomPersonId(), FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, clock.instant());

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class);

    assertThat(logRecorder.asString()).doesNotContain("Admitted on link");
  }

  /** Every call to the registry fails at the connection, as when the registry is down. */
  private static void registryIsDown() {
    stubFor(
        WireMock.any(anyUrl()).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
  }

  /** The registry as the handler has it, but giving up on an answer sooner. */
  private PersonRegistry registryWithRequestTimeout(Duration requestTimeout) {
    return CristinPersonRegistry.customPersonRegistry(
        WiremockHttpClient.create(),
        wiremockUri,
        ServiceConstants.API_DOMAIN,
        defaultRequestHeaders,
        new SecretsReader(secretsManagerClient),
        requestTimeout);
  }
}
