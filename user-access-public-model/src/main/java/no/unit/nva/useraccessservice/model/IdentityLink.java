package no.unit.nva.useraccessservice.model;

import static java.util.Objects.isNull;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import nva.commons.core.StringUtils;

/**
 * The link from a Cognito user to the person it represents. Keyed on the Cognito username, which is
 * stable across logins, and rewritten on every login that resolves the person.
 *
 * @param username the Cognito username, for federated users providerName_sub
 * @param personId the id of the person the account resolved to
 * @param feideIdentifier the Feide identifier of the account, null for accounts without one
 * @param orgFeideDomain the Feide organization domain of the account, null for accounts without one
 * @param modified when the link was last written
 */
public record IdentityLink(
    String username,
    URI personId,
    String feideIdentifier,
    String orgFeideDomain,
    Instant modified) {

  /**
   * How old a link may be when a user is admitted on it because the person registry cannot be
   * queried. A link does not expire: every other reader uses it whatever its age.
   */
  public static final Duration MAX_AGE = Duration.ofDays(30);

  public IdentityLink {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("A link needs a username");
    }
    if (isNull(personId)) {
      throw new IllegalArgumentException("A link needs a person id");
    }
  }

  /**
   * Whether the link was last written more than {@link #MAX_AGE} ago. A link without a time of
   * writing counts as older.
   */
  public boolean isOlderThanMaxAge(Clock clock) {
    return isNull(modified) || modified.isBefore(clock.instant().minus(MAX_AGE));
  }
}
