package no.unit.nva.cognito.inbound;

import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * The claims or attributes from one source in the inbound federation event, with the two ways
 * {@link FederatedLogin} reads a value. Dataporten sends a claim as a string, a multivalued
 * attribute either as a list or as the JSON array text that Cognito also stores, and a few claims
 * such as {@code exp} as numbers. A value may also be null.
 */
final class ProviderClaims {

  private final Map<String, Object> values;

  private ProviderClaims(Map<String, Object> values) {
    this.values = values;
  }

  /** Wraps the raw map from the event without copying it. */
  static ProviderClaims from(Map<String, Object> values) {
    return new ProviderClaims(values);
  }

  /** A single text value, trimmed. Empty when the claim is missing, blank or not text. */
  Optional<String> text(String key) {
    return values.get(key) instanceof String value && !value.isBlank()
        ? Optional.of(value.trim())
        : Optional.empty();
  }

  /**
   * A possibly multivalued attribute before decoding. A list gives its first element as text; a
   * string is passed on untouched, also when it is blank or JSON array text with several values,
   * for {@link no.unit.nva.useraccessservice.usercreation.person.PersonName} to decode. Empty when
   * the attribute is missing, an empty list, a list that starts with null, or neither a list nor a
   * string.
   */
  Optional<String> firstValue(String key) {
    return switch (values.get(key)) {
      case String value -> Optional.of(value);
      case List<?> list when !list.isEmpty() && nonNull(list.getFirst()) ->
          Optional.of(String.valueOf(list.getFirst()));
      case null, default -> Optional.empty();
    };
  }

  /** The claim names in sorted order, for logging what the provider sent without the values. */
  Set<String> keys() {
    return new TreeSet<>(values.keySet());
  }
}
