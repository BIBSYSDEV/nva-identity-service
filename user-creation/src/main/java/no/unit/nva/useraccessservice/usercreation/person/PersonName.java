package no.unit.nva.useraccessservice.usercreation.person;

import static java.util.Objects.nonNull;
import static nva.commons.core.StringUtils.isBlank;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * A person's name as the identity provider or the person registry gives it.
 *
 * @param givenName the given name, possibly several words
 * @param familyName the family name
 */
public record PersonName(String givenName, String familyName) {

  /**
   * Splits a full name the way the pre-token Lambda has since 2022: the last word is the family
   * name, everything before it the given name. A name of one word cannot be split.
   */
  public static Optional<PersonName> fromFullName(String fullName) {
    if (isBlank(fullName)) {
      return Optional.empty();
    }
    var parts = fullName.trim().split("\\s+");
    var last = parts.length - 1;
    return last < 1
        ? Optional.empty()
        : Optional.of(new PersonName(String.join(" ", Arrays.copyOf(parts, last)), parts[last]));
  }

  /**
   * The name from the provider's separate given name and surname attributes, when both are there.
   */
  public static Optional<PersonName> fromProviderNames(String givenName, String familyName) {
    var given = decodeProviderValue(givenName);
    var family = decodeProviderValue(familyName);
    return given.isPresent() && family.isPresent()
        ? Optional.of(new PersonName(given.get(), family.get()))
        : Optional.empty();
  }

  /**
   * A single value as Feide sends it through Dataporten and as Cognito stores it: a multi-valued
   * attribute comes as a JSON array, possibly URL-encoded, such as {@code ["Løk"]} or {@code
   * [%22L%C3%B8k%22]}. Brackets, quotes and commas go, the rest is decoded. A value that cannot be
   * decoded counts as missing.
   */
  public static Optional<String> decodeProviderValue(String value) {
    if (isBlank(value)) {
      return Optional.empty();
    }
    try {
      var unwrapped = stripEnclosing(value.trim(), "[", "]");
      var decoded = URLDecoder.decode(unwrapped, StandardCharsets.UTF_8);
      var result = stripEnclosing(decoded, "\"", "\"").replace(",", "").trim();
      return result.isEmpty() ? Optional.empty() : Optional.of(result);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static String stripEnclosing(String value, String start, String end) {
    var enclosed =
        value.length() >= start.length() + end.length()
            && value.startsWith(start)
            && value.endsWith(end);
    return enclosed ? value.substring(start.length(), value.length() - end.length()) : value;
  }

  /** Whether this is the same name, ignoring case and surrounding space. */
  public boolean matches(String otherGivenName, String otherFamilyName) {
    return sameName(givenName, otherGivenName) && sameName(familyName, otherFamilyName);
  }

  private static boolean sameName(String one, String other) {
    return nonNull(one) && nonNull(other) && one.trim().equalsIgnoreCase(other.trim());
  }
}
