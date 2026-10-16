package no.unit.nva.useraccessservice.model;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

/**
 * What the pre-token Lambda needs to know about a person without asking the person registry. One
 * row per person, shared by all Cognito users that resolve to the same person.
 *
 * @param cristinId the person identifier
 * @param givenName the given name as last answered by the person registry
 * @param familyName the family name as last answered by the person registry
 * @param customers the ids of the customers the person had an active affiliation with at the last
 *     answer from the person registry
 * @param lastVerified when the person registry last confirmed this person
 */
public record LinkedPerson(
    URI cristinId, String givenName, String familyName, Set<URI> customers, Instant lastVerified) {

  public LinkedPerson {
    customers = customers == null ? Set.of() : Set.copyOf(customers);
  }
}
