package no.unit.nva.cognito.inbound;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.EMAIL;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.FIXTURE_FAMILY_NAME;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.FIXTURE_GIVEN_NAME;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.HOME_ORGANIZATION;
import static no.unit.nva.cognito.inbound.InboundFederationEvents.usernameOf;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceLoginNotCompletedException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;

/**
 * Logins that reach the person registry: the person is found or created there, the user entries are
 * made and the link and person rows are written. A failure after the registry has answered fails
 * the login with 1005.
 */
class InboundFederationThroughTheRegistryTest extends InboundFederationHandlerTestBase {

  private static final Duration ONE_HOUR = Duration.ofHours(1);

  @Test
  void shouldCreateUserEntriesAndLinkAndPersonRowsOnFirstLogin() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var cristinId = scenarios.getCristinIdForPerson(person.nin());
    var event = events.feideLogin(person.nin());
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
              assertThat(link.personId()).isEqualTo(cristinId);
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
                + " through the person registry");
  }

  @Test
  void shouldQueryTheRegistryAgainAndWriteTheRowsAgainWhenTheUserHasALink() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var cristinId = scenarios.getCristinIdForPerson(person.nin());
    var event = events.feideLogin(person.nin());
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
  void shouldCreateThePersonWithTheProvidersNameWhenTheRegistryHasNone() {
    var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
    mockPersonRegistry.createPostPersonStub(person.getCristinPerson());
    var event = events.feideLogin(person.nin(), OTHER_GIVEN_NAME, OTHER_FAMILY_NAME);

    handler.handleRequest(event, context);

    WireMock.verify(
        postRequestedFor(urlPathEqualTo("/persons"))
            .withRequestBody(containing(OTHER_GIVEN_NAME))
            .withRequestBody(containing(OTHER_FAMILY_NAME)));
    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::personId)
        .map(URI::toString)
        .get()
        .satisfies(cristinId -> assertThat(cristinId).endsWith(person.cristinIdentifier()));
  }

  @Test
  void shouldWriteALinkWithoutFeideDetailsForAnIdPortenLogin() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var event = events.idPortenLogin(person.nin(), FIXTURE_GIVEN_NAME + " " + FIXTURE_FAMILY_NAME);

    handler.handleRequest(event, context);

    assertThat(identityService.fetchLink(usernameOf(event)))
        .get()
        .satisfies(
            link -> {
              assertThat(link.personId()).isEqualTo(scenarios.getCristinIdForPerson(person.nin()));
              assertThat(link.feideIdentifier()).isNull();
              assertThat(link.orgFeideDomain()).isNull();
            });
    assertThat(scanAllUsers()).singleElement().extracting(UserDto::getFeideIdentifier).isNull();
  }

  @Test
  void shouldRewriteTheLinkAndLogItWhenTheRegistryAnswersAnotherPerson() {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var cristinId = scenarios.getCristinIdForPerson(person.nin());
    var event = events.feideLogin(person.nin());
    var previousCristinId = randomPersonId();
    linkPerson(event, previousCristinId, FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME, clock.instant());

    handler.handleRequest(event, context);

    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::personId)
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
    var event = events.feideLogin(person.nin());

    handler.handleRequest(event, context);

    assertThat(scanAllUsers()).isEmpty();
    assertThat(identityService.fetchLinkedPerson(scenarios.getCristinIdForPerson(person.nin())))
        .map(LinkedPerson::customers)
        .contains(Set.of());
    assertThat(logRecorder.asString()).contains("No customer for institution");
  }

  @Test
  void shouldTreatAnExistingUserEntryAsFoundWhenTheIndexMissesIt() throws ConflictException {
    var person = scenarios.personWithExactlyOneActiveEmployment();
    var customer = scenarios.fetchCustomersForPerson(person.nin()).getFirst();
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
    var event = events.feideLogin(person.nin());

    handler.handleRequest(event, context);

    assertThat(scanAllUsers()).hasSize(1);
    assertThat(identityService.fetchLink(usernameOf(event)))
        .map(IdentityLink::personId)
        .contains(scenarios.getCristinIdForPerson(person.nin()));
  }

  @Test
  void shouldAnswerLoginNotCompletedWhenTheRowsCannotBeWritten() {
    var failingIdentityService = spy(identityService);
    doThrow(new RuntimeException("Table unavailable")).when(failingIdentityService).saveLink(any());
    var failing =
        new InboundFederationHandler(
            customerService, failingIdentityService, personRegistry, clock);
    var event = events.feideLogin(scenarios.personWithExactlyOneActiveEmployment().nin());

    assertThatThrownBy(() -> failing.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class)
        .hasMessageContaining(LOGIN_NOT_COMPLETED_CODE);

    assertThat(logRecorder.asString())
        .contains("Inbound federation for user " + usernameOf(event) + " failed");
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
    var event = events.feideLogin(scenarios.personWithExactlyOneActiveEmployment().nin());

    assertThatThrownBy(() -> failing.handleRequest(event, context))
        .isInstanceOf(IdentityServiceLoginNotCompletedException.class);

    assertThat(identityService.fetchLink(usernameOf(event))).isEmpty();
  }

  private static Set<URI> customerIds(List<CustomerDto> customers) {
    return customers.stream().map(CustomerDto::getId).collect(Collectors.toSet());
  }
}
