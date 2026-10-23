package no.unit.nva.useraccessservice.usercreation;

import static org.assertj.core.api.Assertions.assertThat;

import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class NationalIdentityNumberTest {

  private static final String NUMBER = "01819000049";

  @Test
  void shouldNeverPrintTheNumber() {
    assertThat(NationalIdentityNumber.fromString(NUMBER).toString())
        .doesNotContain(NUMBER)
        .doesNotContain("01")
        .contains("NationalIdentityNumber");
  }

  @Test
  void shouldAcceptElevenDigits() {
    assertThat(NationalIdentityNumber.fromDigits(NUMBER))
        .map(NationalIdentityNumber::getNin)
        .contains(NUMBER);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "0181900004", "018190000491", "0181900004a", "01819000049 "})
  void shouldRejectAnythingButElevenDigits(String value) {
    assertThat(NationalIdentityNumber.fromDigits(value)).isEmpty();
  }

  @Test
  void shouldMaskEveryNumberInAText() {
    var text =
        "GET /persons/resolve?national_id=" + NUMBER + " and body {\"nin\":\"" + NUMBER + "\"}";

    var masked = NationalIdentityNumber.maskAll(text);

    assertThat(masked)
        .doesNotContain(NUMBER)
        .isEqualTo(
            "GET /persons/resolve?national_id=*********** and body {\"nin\":\"***********\"}");
  }

  @Test
  void shouldLeaveTextWithoutNumbersAlone() {
    var text = "No person found for identifier 1234567890 (ten digits)";

    assertThat(NationalIdentityNumber.maskAll(text)).isEqualTo(text);
  }
}
