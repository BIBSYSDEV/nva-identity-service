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
    assertThat(name.matches("Kari", "Nordmann Hansen")).isFalse();
    assertThat(name.matches(null, "Nordmann")).isFalse();
  }
}
