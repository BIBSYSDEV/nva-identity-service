package no.unit.nva.useraccessservice.model;

import static java.util.Objects.isNull;

/**
 * A Cognito user's link together with the person it points to. The two are separate rows, so either
 * can be missing; this type exists only when both were found.
 *
 * @param link the link from the Cognito user to the person
 * @param person what is known about that person
 */
public record LinkedIdentity(IdentityLink link, LinkedPerson person) {

  public LinkedIdentity {
    if (isNull(link) || isNull(person)) {
      throw new IllegalArgumentException("A linked identity needs both the link and the person");
    }
  }
}
