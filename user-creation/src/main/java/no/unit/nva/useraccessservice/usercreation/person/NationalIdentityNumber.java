package no.unit.nva.useraccessservice.usercreation.person;

import java.util.Optional;
import java.util.regex.Pattern;
import nva.commons.core.JacocoGenerated;

/**
 * A Norwegian national identity number. It never prints itself, and {@link #maskAll} hides every
 * number in a text that is about to be logged or put in a message.
 */
public final class NationalIdentityNumber {

  private static final Pattern ELEVEN_DIGITS = Pattern.compile("\\d{11}");

  /** Eleven digits, also written with a space or a hyphen after the birthdate, as a person may. */
  private static final Pattern NUMBER_IN_TEXT = Pattern.compile("\\d{6}[ -]?\\d{5}");

  private static final String MASK = "***********";
  private final String nin;

  private NationalIdentityNumber(final String nin) {
    this.nin = nin;
  }

  /** Wraps the value as it is, for callers that already know it is a number. */
  public static NationalIdentityNumber fromString(final String nin) {
    return new NationalIdentityNumber(nin);
  }

  /** The value when it is eleven digits, otherwise empty: nothing else can be looked up. */
  public static Optional<NationalIdentityNumber> fromDigits(final String value) {
    return Optional.ofNullable(value)
        .filter(candidate -> ELEVEN_DIGITS.matcher(candidate).matches())
        .map(NationalIdentityNumber::new);
  }

  /**
   * The text with every number masked, for logs and messages. A number written with a separator
   * after the birthdate is masked too, since a hand-typed value may be echoed by the registry.
   */
  public static String maskAll(final String text) {
    return NUMBER_IN_TEXT.matcher(text).replaceAll(MASK);
  }

  public String getNin() {
    return nin;
  }

  @Override
  public String toString() {
    return "NationalIdentityNumber{" + MASK + "}";
  }

  @JacocoGenerated
  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof NationalIdentityNumber that && nin.equals(that.nin);
  }

  @JacocoGenerated
  @Override
  public int hashCode() {
    return nin.hashCode();
  }
}
