package no.unit.nva.useraccessservice.usercreation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import no.unit.nva.useraccessservice.usercreation.person.PersonName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PersonNameTest {

  static Stream<Arguments> fullNames() {
    return Stream.of(
        Arguments.of("Trude Ellerei", "Trude", "Ellerei"),
        Arguments.of("Frode Kommode Osteloffsen", "Frode Kommode", "Osteloffsen"),
        Arguments.of("O'Connor McGregor", "O'Connor", "McGregor"),
        Arguments.of("  Kari   Nordmann  ", "Kari", "Nordmann"),
        Arguments.of("Pål-Ove Sitter-Nissen", "Pål-Ove", "Sitter-Nissen"));
  }

  static Stream<Arguments> sameNameWrittenDifferently() {
    return Stream.of(
        Arguments.of(
            "middle name only in the other",
            new PersonName("Kari", "Nordmann"),
            new PersonName("Kari Anne", "Nordmann")),
        Arguments.of(
            "middle name only in this",
            new PersonName("Kari Anne", "Nordmann"),
            new PersonName("Kari", "Nordmann")),
        Arguments.of(
            "first given name missing in the other",
            new PersonName("Kari Anne", "Nordmann"),
            new PersonName("Anne", "Nordmann")),
        Arguments.of(
            "double family name split differently",
            new PersonName("Kari", "Hansen Nordmann"),
            new PersonName("Kari Hansen", "Nordmann")),
        Arguments.of(
            "middle name placed in the family name",
            new PersonName("Kari Anne", "Nordmann"),
            new PersonName("Kari", "Anne Nordmann")));
  }

  static Stream<Arguments> differentNames() {
    return Stream.of(
        Arguments.of(
            "another given name",
            new PersonName("Kari", "Nordmann"),
            new PersonName("Ola", "Nordmann")),
        Arguments.of(
            "another family name",
            new PersonName("Kari", "Nordmann"),
            new PersonName("Kari", "Hansen")),
        Arguments.of(
            "family names ending in different words",
            new PersonName("Kari", "Nordmann"),
            new PersonName("Kari", "Nordmann Hansen")),
        Arguments.of(
            "each has a middle name the other lacks",
            new PersonName("Kari Anne", "Nordmann"),
            new PersonName("Kari Marie", "Nordmann")),
        Arguments.of(
            "given name and family name swapped",
            new PersonName("Kari", "Nordmann"),
            new PersonName("Nordmann", "Kari")));
  }

  @ParameterizedTest
  @MethodSource("fullNames")
  void shouldTakeTheLastWordAsFamilyName(String fullName, String givenName, String familyName) {
    assertThat(PersonName.fromFullName(fullName)).contains(new PersonName(givenName, familyName));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "Julenissen", "  Kongen  "})
  void shouldNotSplitANameOfOneWordOrLess(String fullName) {
    assertThat(PersonName.fromFullName(fullName)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Løk", "[\"Løk\"]", "[%22L%C3%B8k%22]", " [\"Løk\"] "})
  void shouldDecodeAValueAsFeideSendsItAndCognitoStoresIt(String value) {
    assertThat(PersonName.decodeProviderValue(value)).contains("Løk");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "[]", "[\"\"]", "%E0%A4%A"})
  void shouldTreatAnEmptyOrUndecodableValueAsMissing(String value) {
    assertThat(PersonName.decodeProviderValue(value)).isEmpty();
  }

  @Test
  void shouldBuildTheNameFromTheProvidersSeparateAttributes() {
    assertThat(PersonName.fromProviderNames("[\"Kari\"]", "Nordmann"))
        .contains(new PersonName("Kari", "Nordmann"));
    assertThat(PersonName.fromProviderNames("Kari", null)).isEmpty();
    assertThat(PersonName.fromProviderNames("[]", "Nordmann")).isEmpty();
  }

  @Test
  void shouldMatchIgnoringCaseAndSurroundingSpace() {
    var name = new PersonName("Kari", "Nordmann");

    assertThat(name.matches(" kari ", "NORDMANN")).isTrue();
    assertThat(name.matches("Kari\t", "Nordmann")).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("sameNameWrittenDifferently")
  void shouldMatchTheSameNameWrittenDifferently(
      String difference, PersonName name, PersonName other) {
    assertThat(name.matches(other.givenName(), other.familyName())).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("differentNames")
  void shouldNotMatchDifferentName(String difference, PersonName name, PersonName other) {
    assertThat(name.matches(other.givenName(), other.familyName())).isFalse();
  }

  @Test
  void shouldNotMatchWhenAPartOfEitherNameIsMissing() {
    var name = new PersonName("Kari", "Nordmann");

    assertThat(name.matches(null, "Nordmann")).isFalse();
    assertThat(name.matches("Kari", " ")).isFalse();
    assertThat(new PersonName(null, "Nordmann").matches("Kari", "Nordmann")).isFalse();
    assertThat(new PersonName("Kari", " ").matches("Kari", "Nordmann")).isFalse();
  }
}
