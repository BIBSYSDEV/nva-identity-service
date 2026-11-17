package no.unit.nva.useraccessservice.model;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IdentityLinkTest {

  private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

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

  @Test
  void shouldNotBeOlderThanMaxAgeWhenWrittenWithinIt() {
    var link = linkWrittenAt(NOW.minus(IdentityLink.MAX_AGE).plus(Duration.ofHours(1)));

    assertThat(link.isOlderThanMaxAge(CLOCK), is(false));
  }

  @Test
  void shouldNotBeOlderThanMaxAgeWhenWrittenExactlyMaxAgeAgo() {
    var link = linkWrittenAt(NOW.minus(IdentityLink.MAX_AGE));

    assertThat(link.isOlderThanMaxAge(CLOCK), is(false));
  }

  @Test
  void shouldBeOlderThanMaxAgeWhenWrittenBeforeIt() {
    var link = linkWrittenAt(NOW.minus(IdentityLink.MAX_AGE).minus(Duration.ofHours(1)));

    assertThat(link.isOlderThanMaxAge(CLOCK), is(true));
  }

  @Test
  void shouldBeOlderThanMaxAgeWithoutATimeOfWriting() {
    assertThat(linkWrittenAt(null).isOlderThanMaxAge(CLOCK), is(true));
  }

  private static IdentityLink linkWrittenAt(Instant modified) {
    return new IdentityLink(randomString(), randomUri(), randomString(), randomString(), modified);
  }
}
