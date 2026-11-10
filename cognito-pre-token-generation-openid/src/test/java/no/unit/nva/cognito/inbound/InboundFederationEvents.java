package no.unit.nva.cognito.inbound;

import static no.unit.nva.cognito.inbound.FederatedLogin.FEIDE_NIN_ATTRIBUTE;
import static no.unit.nva.cognito.inbound.FederatedLogin.FULL_NAME_ATTRIBUTE;
import static no.unit.nva.cognito.inbound.FederatedLogin.GIVEN_NAME_ATTRIBUTE;
import static no.unit.nva.cognito.inbound.FederatedLogin.NIN_CLAIM;
import static no.unit.nva.cognito.inbound.FederatedLogin.PRINCIPAL_NAME_CLAIM;
import static no.unit.nva.cognito.inbound.FederatedLogin.SURNAME_ATTRIBUTE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import nva.commons.core.ioutils.IoUtils;

/**
 * Inbound federation events as Dataporten sends them, built from the JSON fixture. Every event
 * built is remembered, so that a test can check afterwards that nothing the provider sent was
 * logged.
 */
class InboundFederationEvents {

  static final String USERNAME_PREFIX = "Dataporten_";
  static final String EMAIL = "example@example.no";
  static final String HOME_ORGANIZATION = "example.no";
  static final String FIXTURE_GIVEN_NAME = "Exemplaria";
  static final String FIXTURE_FAMILY_NAME = "Fixturesen";
  static final Path EVENT_RESOURCE = Path.of("cognito", "inbound_federation_event.json");

  /**
   * Plain Jackson, configured like the Lambda runtime's serializer: unknown properties are ignored
   * and nothing else is special-cased.
   */
  static final JsonMapper RUNTIME_LIKE_MAPPER =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private static final String SUB = "sub";

  private final List<InboundFederationEvent> built = new ArrayList<>();

  InboundFederationEvent feideLogin(String nin) {
    return feideLogin(nin, FIXTURE_GIVEN_NAME, FIXTURE_FAMILY_NAME);
  }

  /** The fixture as Feide sends it, with this person's number and name. */
  InboundFederationEvent feideLogin(String nin, String givenName, String familyName) {
    var event = fromFixture();
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
  InboundFederationEvent idPortenLogin(String nin, String fullName) {
    var event = fromFixture();
    event.setUserName(randomString());
    attributesOf(event).getUserInfo().clear();
    attributesOf(event).getIdToken().remove(PRINCIPAL_NAME_CLAIM);
    attributesOf(event).getIdToken().put(NIN_CLAIM, nin);
    attributesOf(event).getIdToken().put(FULL_NAME_ATTRIBUTE, fullName);
    return remember(event);
  }

  /**
   * Every non-blank string and every number the provider sent in the events built so far, also
   * inside nested maps and lists, except the subject, which is part of the username and is meant to
   * be logged.
   */
  List<String> valuesSent() {
    var values = new ArrayList<String>();
    built.forEach(event -> collectValues(event, values));
    return values;
  }

  static InboundFederationEvent fromFixture() {
    try {
      return RUNTIME_LIKE_MAPPER.readValue(
          IoUtils.stringFromResources(EVENT_RESOURCE), InboundFederationEvent.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  static InboundFederationEvent.Attributes attributesOf(InboundFederationEvent event) {
    return event.getRequest().getAttributes();
  }

  /** The Cognito username of the event's user: the provider name and the provider's subject. */
  static String usernameOf(InboundFederationEvent event) {
    return USERNAME_PREFIX + event.getUserName();
  }

  private InboundFederationEvent remember(InboundFederationEvent event) {
    built.add(event);
    return event;
  }

  private static void collectValues(InboundFederationEvent event, List<String> values) {
    var attributes = attributesOf(event);
    Stream.of(attributes.getTokenResponse(), attributes.getIdToken(), attributes.getUserInfo())
        .forEach(map -> collectValues(map, values));
  }

  private static void collectValues(Map<String, Object> attributes, List<String> values) {
    attributes.forEach(
        (key, value) -> {
          if (!SUB.equals(key)) {
            collectValue(value, values);
          }
        });
  }

  private static void collectValue(Object value, List<String> values) {
    if (value instanceof Map<?, ?> map) {
      map.values().forEach(nested -> collectValue(nested, values));
    } else if (value instanceof List<?> list) {
      list.forEach(nested -> collectValue(nested, values));
    } else if (isNumberOrNonBlankText(value)) {
      values.add(String.valueOf(value));
    }
  }

  private static boolean isNumberOrNonBlankText(Object value) {
    return value instanceof Number || (value instanceof String text && !text.isBlank());
  }
}
