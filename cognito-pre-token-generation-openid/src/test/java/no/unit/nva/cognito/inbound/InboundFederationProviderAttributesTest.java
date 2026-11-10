package no.unit.nva.cognito.inbound;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static no.unit.nva.cognito.inbound.FederatedLogin.FEIDE_NIN_ATTRIBUTE;
import static no.unit.nva.cognito.inbound.FederatedLogin.GIVEN_NAME_ATTRIBUTE;
import static no.unit.nva.cognito.inbound.FederatedLogin.NIN_CLAIM;
import static no.unit.nva.cognito.inbound.FederatedLogin.SURNAME_ATTRIBUTE;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.EMAIL;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.FIXTURE_FAMILY_NAME;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.FIXTURE_GIVEN_NAME;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.USERNAME_PREFIX;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.attributesOf;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.usernameOf;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.userceation.testing.cristin.RandomNin.randomNin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import java.util.Optional;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceLoginNotCompletedException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceMissingNinException;
import org.junit.jupiter.api.Test;

/**
 * How the login is read from what the provider sent: the number, the name, the username, and what
 * of it may ever be printed.
 */
class InboundFederationProviderAttributesTest extends InboundFederationHandlerTestBase {

  private static final String NINE_DIGITS = "123456789";

  @Test
  void shouldFailWithMissingNinWithoutCallingTheRegistryWhenTheProviderSendsNoNumber() {
    var event = events.feideLogin(scenarios.personWithExactlyOneActiveEmployment().nin());
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
  void shouldTreatANumberThatIsNotElevenDigitsAsMissing() {
    var event = events.feideLogin(NINE_DIGITS);

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceMissingNinException.class);

    WireMock.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void shouldReadTheNumberFromUserInfoWhenTheIdTokenHasNone() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var event = events.feideLogin(person.nin());
    attributesOf(event).getIdToken().remove(NIN_CLAIM);

    handler.handleRequest(event, context);

    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::personId)
        .contains(scenarios.getCristinIdForPerson(person.nin()));
  }

  @Test
  void shouldPreferTheNumberFromUserInfoOverTheIdTokenClaim() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var event = events.feideLogin(person.nin());
    attributesOf(event).getIdToken().put(NIN_CLAIM, randomNin());

    handler.handleRequest(event, context);

    WireMock.verify(
        getRequestedFor(urlPathMatching("/persons/resolve"))
            .withQueryParam("national_id", equalTo(person.nin())));
  }

  @Test
  void shouldFailWhenThePersonMustBeCreatedAndTheProviderSendsNoUsableName() {
    var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
    mockPersonRegistry.createPostPersonStub(person.getCristinPerson());
    var event = events.idPortenLogin(person.nin(), FIXTURE_GIVEN_NAME);

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class)
        .hasMessageContaining(LOGIN_NOT_COMPLETED_CODE);

    WireMock.verify(0, postRequestedFor(anyUrl()));
    assertThat(logRecorder.asString())
        .contains("No name from Dataporten for user " + usernameOf(event));
  }

  @Test
  void shouldDecodeNamesAsFeideSendsThemBeforeCreatingThePerson() {
    var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
    mockPersonRegistry.createPostPersonStub(person.getCristinPerson());
    var event = events.feideLogin(person.nin(), "[%22Kari%22]", "[\"Nordmann\"]");

    handler.handleRequest(event, context);

    WireMock.verify(
        postRequestedFor(urlPathEqualTo("/persons"))
            .withRequestBody(containing("\"Kari\""))
            .withRequestBody(containing("\"Nordmann\"")));
  }

  @Test
  void shouldMatchTheRegistryNameWhenFeideSendsTheNameAsAList() {
    var registry = registryAnswering(FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME);
    var event = events.feideLogin(randomNin());
    attributesOf(event).getUserInfo().put(GIVEN_NAME_ATTRIBUTE, List.of(FIXTURE_GIVEN_NAME));
    attributesOf(event).getUserInfo().put(SURNAME_ATTRIBUTE, List.of(FIXTURE_FAMILY_NAME));

    handlerWith(registry, clock).handleRequest(event, context);

    assertThat(logRecorder.asString()).contains("matches the registry: true");
  }

  @Test
  void shouldLogThatTheNameDoesNotMatchWhenTheRegistryKnowsThePersonByAnotherName() {
    var registry = registryAnswering(OTHER_GIVEN_NAME, OTHER_FAMILY_NAME);
    var event = events.feideLogin(randomNin());

    handlerWith(registry, clock).handleRequest(event, context);

    assertThat(logRecorder.asString())
        .contains(
            "Name from Dataporten for user " + usernameOf(event) + " matches the registry: false");
  }

  @Test
  void shouldNotDoubleTheProviderPrefixWhenTheEventCarriesTheCognitoUsername() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var event = events.feideLogin(person.nin());
    event.setUserName(USERNAME_PREFIX + event.getUserName());

    handler.handleRequest(event, context);

    assertThat(identityService.fetchLink(event.getUserName())).isPresent();
    assertThat(identityService.fetchLink(USERNAME_PREFIX + event.getUserName())).isEmpty();
  }

  @Test
  void shouldPrintNothingButTheUsernameForALogin() {
    var nin = randomNin();
    var event = events.feideLogin(nin);

    assertThat(FederatedLogin.from(event).toString())
        .contains(usernameOf(event))
        .doesNotContain(nin, FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, EMAIL);
  }

  private static PersonRegistry registryAnswering(String givenName, String familyName) {
    var registry = mock(PersonRegistry.class);
    when(registry.fetchPersonByNin(any()))
        .thenReturn(
            Optional.of(
                new Person(randomPersonId(), randomString(), givenName, familyName, List.of())));
    return registry;
  }
}
