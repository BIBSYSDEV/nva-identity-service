package no.unit.nva.useraccessservice.usercreation.person;

import static java.util.Objects.nonNull;
import static nva.commons.core.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Optional;

/**
 * A person's name as the identity provider or the person registry gives it.
 *
 * @param givenName the given name, possibly several words
 * @param familyName the family name
 */
public record PersonName(String givenName, String familyName) {

  private static final String WHITESPACE = "\\s+";
  private static final String SPACE = " ";
  private static final int ONE = 1;

  /**
   * Splits a full name the way the pre-token Lambda has since 2022: the last word is the family
   * name, everything before it the given name. A name of one word cannot be split.
   */
  public static Optional<PersonName> fromFullName(String fullName) {
    if (isBlank(fullName)) {
      return Optional.empty();
    }
    var parts = fullName.trim().split(WHITESPACE);
    if (parts.length <= ONE) {
      return Optional.empty();
    }
    var givenName = String.join(SPACE, Arrays.copyOf(parts, parts.length - ONE));
    return Optional.of(new PersonName(givenName, parts[parts.length - ONE]));
  }

  /** Whether this is the same name, ignoring case and surrounding space. */
  public boolean matches(String otherGivenName, String otherFamilyName) {
    return sameName(givenName, otherGivenName) && sameName(familyName, otherFamilyName);
  }

  private static boolean sameName(String one, String other) {
    return nonNull(one) && nonNull(other) && one.trim().equalsIgnoreCase(other.trim());
  }
}
