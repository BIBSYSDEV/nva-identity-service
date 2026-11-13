package no.unit.nva.useraccessservice.model;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LinkedIdentityTest {

  private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

  @Test
  void shouldRejectLinkedIdentityWithoutLink() {
    var person = new LinkedPerson(randomUri(), randomString(), randomString(), Set.of(), NOW);

    assertThrows(IllegalArgumentException.class, () -> new LinkedIdentity(null, person));
  }

  @Test
  void shouldRejectLinkedIdentityWithoutPerson() {
    var link = new IdentityLink(randomString(), randomUri(), null, null, NOW);

    assertThrows(IllegalArgumentException.class, () -> new LinkedIdentity(link, null));
  }
}
