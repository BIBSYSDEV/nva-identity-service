package no.unit.nva.useraccessservice.model;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LinkedPersonTest {

  @Test
  void shouldRejectMissingPersonId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new LinkedPerson(null, randomString(), randomString(), Set.of(), Instant.now()));
  }

  @Test
  void shouldTreatMissingCustomersAsNone() {
    var person = new LinkedPerson(randomUri(), randomString(), randomString(), null, Instant.now());

    assertThat(person.customers(), is(empty()));
  }

  @Test
  void shouldKeepItsOwnCopyOfTheCustomers() {
    var customer = randomUri();
    var customers = new HashSet<>(Set.of(customer));
    var person =
        new LinkedPerson(randomUri(), randomString(), randomString(), customers, Instant.now());

    customers.add(randomUri());

    assertThat(person.customers(), contains(customer));
  }
}
