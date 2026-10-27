package no.unit.nva.useraccessservice.model;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IdentityLinkTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldRejectMissingUsername(String username) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new IdentityLink(username, randomUri(), null, null, Instant.now()));
  }

  @Test
  void shouldRejectMissingPersonId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new IdentityLink(randomString(), null, null, null, Instant.now()));
  }

  @Test
  void shouldAllowMissingFeideFields() {
    var link = new IdentityLink(randomString(), randomUri(), null, null, Instant.now());

    assertThat(link.feideIdentifier(), is(nullValue()));
    assertThat(link.orgFeideDomain(), is(nullValue()));
  }
}
