package no.unit.nva.cognito.inbound;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProviderClaimsTest {

  @Test
  void shouldReadTextTrimmed() {
    var claims = ProviderClaims.from(Map.of("name", "  Kari Nordmann "));

    assertThat(claims.text("name"), is(Optional.of("Kari Nordmann")));
  }

  @Test
  void shouldTreatBlankMissingAndNonTextAsNoText() {
    var values = new HashMap<String, Object>();
    values.put("blank", " ");
    values.put("number", 1700000000);
    values.put("absent", null);
    var claims = ProviderClaims.from(values);

    assertThat(claims.text("blank"), is(Optional.empty()));
    assertThat(claims.text("number"), is(Optional.empty()));
    assertThat(claims.text("absent"), is(Optional.empty()));
    assertThat(claims.text("missing"), is(Optional.empty()));
  }

  @Test
  void shouldPassTextOnUntouchedAsFirstValue() {
    var claims = ProviderClaims.from(Map.of("sn", "[%22L%C3%B8k%22]", "givenName", " Kari "));

    assertThat(claims.firstValue("sn"), is(Optional.of("[%22L%C3%B8k%22]")));
    assertThat(claims.firstValue("givenName"), is(Optional.of(" Kari ")));
  }

  @Test
  void shouldReduceListToItsFirstElementAsText() {
    var claims =
        ProviderClaims.from(
            Map.of("givenName", List.of("Kari", "Karianne"), "numbers", List.of(42)));

    assertThat(claims.firstValue("givenName"), is(Optional.of("Kari")));
    assertThat(claims.firstValue("numbers"), is(Optional.of("42")));
  }

  @Test
  void shouldGiveNoFirstValueForEmptyListNullElementOrNumber() {
    var claims =
        ProviderClaims.from(
            Map.of("empty", List.of(), "nulls", Collections.singletonList(null), "number", 42));

    assertThat(claims.firstValue("empty"), is(Optional.empty()));
    assertThat(claims.firstValue("nulls"), is(Optional.empty()));
    assertThat(claims.firstValue("number"), is(Optional.empty()));
    assertThat(claims.firstValue("missing"), is(Optional.empty()));
  }

  @Test
  void shouldListKeysInOrderWithoutValues() {
    var claims = ProviderClaims.from(Map.of("sub", "x", "exp", 1, "aud", "y"));

    assertThat(claims.keys(), contains("aud", "exp", "sub"));
  }
}
