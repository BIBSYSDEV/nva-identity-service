package no.unit.nva.cognito.inbound;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phase 0 prototype of the inbound federation trigger (NP-51967). It logs which attribute keys the
 * identity provider sends, never their values, and by default returns an empty mapping, which tells
 * Cognito to keep all attributes unchanged.
 *
 * <p>Two environment variables drive the experiments in dev:
 *
 * <ul>
 *   <li>{@code INBOUND_DROP_KEYS}: comma separated attribute keys to leave out of the returned
 *       mapping. When set, the response lists every scalar attribute except these.
 *   <li>{@code INBOUND_FAIL_MESSAGE}: when set, the trigger throws with this message, to see how an
 *       error reaches the user.
 * </ul>
 *
 * <p>Any other failure in this handler is logged and answered with an empty mapping, so that a
 * surprise in the event shape degrades to "keep all attributes" instead of a failed login.
 */
public class InboundFederationHandler
    implements RequestHandler<InboundFederationEvent, InboundFederationEvent> {

  public static final String DROP_KEYS_ENV = "INBOUND_DROP_KEYS";
  public static final String FAIL_MESSAGE_ENV = "INBOUND_FAIL_MESSAGE";

  /** The identity provider attributes that the Dataporten mapping in the template refers to. */
  public static final List<String> MAPPED_IDP_ATTRIBUTES =
      List.of(
          "https://n.feide.no/claims/nin",
          "https://n.feide.no/claims/eduPersonPrincipalName",
          "eduPersonAffiliation",
          "norEduPersonNIN",
          "schacHomeOrganization",
          "givenName",
          "sn",
          "name");

  private static final Logger LOGGER = LoggerFactory.getLogger(InboundFederationHandler.class);
  private static final String KEY_SEPARATOR = ",";

  private final Set<String> keysToDrop;
  private final Optional<String> failMessage;

  @JacocoGenerated
  public InboundFederationHandler() {
    this(new Environment());
  }

  public InboundFederationHandler(Environment environment) {
    this(
        environment
            .readEnvOpt(DROP_KEYS_ENV)
            .map(InboundFederationHandler::splitKeys)
            .orElse(Set.of()),
        environment.readEnvOpt(FAIL_MESSAGE_ENV));
  }

  public InboundFederationHandler(Set<String> keysToDrop, Optional<String> failMessage) {
    this.keysToDrop = Set.copyOf(keysToDrop);
    this.failMessage = failMessage;
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException") // a surprise must not fail the login
  public InboundFederationEvent handleRequest(InboundFederationEvent event, Context context) {
    var start = Instant.now();
    try {
      describe(event);
    } catch (RuntimeException e) {
      LOGGER.error("Could not describe the event", e);
    }

    if (failMessage.isPresent()) {
      LOGGER.warn("Failing on purpose with message: {}", failMessage.get());
      throw new IllegalStateException(failMessage.get());
    }

    Map<String, String> mapping;
    try {
      mapping = attributesToMap(event.getRequest().getAttributes());
    } catch (RuntimeException e) {
      LOGGER.error("Could not build the mapping, keeping all attributes", e);
      mapping = new HashMap<>();
    }
    event.getResponse().setUserAttributesToMap(mapping);
    LOGGER.info(
        "Inbound federation done in {} ms, returning {} attribute(s): {}",
        Instant.now().toEpochMilli() - start.toEpochMilli(),
        mapping.size(),
        new TreeSet<>(mapping.keySet()));
    return event;
  }

  private static void describe(InboundFederationEvent event) {
    var request = event.getRequest();
    LOGGER.info(
        "Inbound federation for {}: provider {} ({}), trigger {}, version {}",
        event.getUserName(),
        request.getProviderName(),
        request.getProviderType(),
        event.getTriggerSource(),
        event.getVersion());
    var attributes = request.getAttributes();
    logKeys("tokenResponse", attributes.getTokenResponse());
    logKeys("idToken", attributes.getIdToken());
    logKeys("userInfo", attributes.getUserInfo());
    logKeys("samlResponse", attributes.getSamlResponse());
    logMappedAttributePresence("idToken", attributes.getIdToken());
    logMappedAttributePresence("userInfo", attributes.getUserInfo());
  }

  private static Set<String> splitKeys(String value) {
    return Arrays.stream(value.split(KEY_SEPARATOR))
        .map(String::trim)
        .filter(key -> !key.isEmpty())
        .collect(Collectors.toSet());
  }

  private static void logKeys(String name, Map<String, Object> values) {
    if (values.isEmpty()) {
      LOGGER.info("{}: empty", name);
      return;
    }
    var nonScalar = new TreeMap<String, String>();
    for (var entry : values.entrySet()) {
      if (!isScalar(entry.getValue())) {
        nonScalar.put(entry.getKey(), typeName(entry.getValue()));
      }
    }
    LOGGER.info(
        "{}: {} key(s) {}, non-scalar {}",
        name,
        values.size(),
        new TreeSet<>(values.keySet()),
        nonScalar);
  }

  private static void logMappedAttributePresence(String name, Map<String, Object> values) {
    var present =
        MAPPED_IDP_ATTRIBUTES.stream().filter(key -> hasNonBlankValue(values, key)).toList();
    LOGGER.info("{}: mapped attributes with a non-blank value: {}", name, present);
  }

  private static boolean hasNonBlankValue(Map<String, Object> values, String key) {
    return values.get(key) instanceof String value && !value.isBlank();
  }

  private static boolean isScalar(Object value) {
    return value instanceof String || value instanceof Number || value instanceof Boolean;
  }

  private static String typeName(Object value) {
    return value == null ? "null" : value.getClass().getSimpleName();
  }

  private Map<String, String> attributesToMap(InboundFederationEvent.Attributes attributes) {
    if (keysToDrop.isEmpty()) {
      return new HashMap<>();
    }
    var merged = new HashMap<String, Object>();
    merged.putAll(attributes.getUserInfo());
    merged.putAll(attributes.getIdToken());
    var dropped = new TreeSet<>(merged.keySet());
    dropped.retainAll(keysToDrop);
    var mapping = new HashMap<String, String>();
    var notScalar = new TreeSet<String>();
    for (var entry : merged.entrySet()) {
      if (keysToDrop.contains(entry.getKey())) {
        continue;
      }
      if (isScalar(entry.getValue())) {
        mapping.put(entry.getKey(), String.valueOf(entry.getValue()));
      } else {
        notScalar.add(entry.getKey());
      }
    }
    LOGGER.info("Dropping {} from the mapping, not mapping non-scalar {}", dropped, notScalar);
    return mapping;
  }
}
