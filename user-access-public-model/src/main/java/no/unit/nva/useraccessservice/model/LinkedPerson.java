package no.unit.nva.useraccessservice.model;

import static java.util.Objects.isNull;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

/**
 * What the pre-token Lambda needs to know about a person without asking the person registry. One
 * row per person, shared by all Cognito users that resolve to the same person.
 *
 * @param personId the person id
 * @param givenName the given name as last answered by the person registry
 * @param familyName the family name as last answered by the person registry
 * @param customers the ids of the customers the person had an active affiliation with at the last
 *     answer from the person registry
 * @param lastVerified when the person registry last confirmed this person
 */
public record LinkedPerson(
    URI personId, String givenName, String familyName, Set<URI> customers, Instant lastVerified) {

  public LinkedPerson {
    if (isNull(personId)) {
      throw new IllegalArgumentException("A linked person needs a person id");
    }
    customers = isNull(customers) ? Set.of() : Set.copyOf(customers);
  }
}
