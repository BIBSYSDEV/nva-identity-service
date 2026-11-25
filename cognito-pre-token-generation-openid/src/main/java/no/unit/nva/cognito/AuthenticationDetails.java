package no.unit.nva.cognito;

import no.unit.nva.useraccessservice.model.IdentityLink;
import nva.commons.core.JacocoGenerated;

public class AuthenticationDetails {
  private final String feideIdentifier;
  private final String feideDomain;
  private final String userPoolId;
  private final String username;
  private final boolean federated;

  public AuthenticationDetails(
      String feideIdentifier,
      String feideDomain,
      String userPoolId,
      String username,
      boolean federated) {
    this.feideIdentifier = feideIdentifier;
    this.feideDomain = feideDomain;
    this.userPoolId = userPoolId;
    this.username = username;
    this.federated = federated;
  }

  public String getFeideIdentifier() {
    return feideIdentifier;
  }

  public String getFeideDomain() {
    return feideDomain;
  }

  public String getUserPoolId() {
    return userPoolId;
  }

  public String getUsername() {
    return username;
  }

  /** True for users that signed in through an identity provider, false for password users. */
  public boolean isFederated() {
    return federated;
  }

  /** The same login, with the Feide details the link remembered instead of the user attributes. */
  public AuthenticationDetails withFeideDetailsFrom(IdentityLink link) {
    return new AuthenticationDetails(
        link.feideIdentifier(), link.orgFeideDomain(), userPoolId, username, federated);
  }

  @JacocoGenerated
  @Override
  public String toString() {
    return "AuthenticationDetails{"
        + "feideIdentifier='"
        + feideIdentifier
        + '\''
        + ", feideDomain='"
        + feideDomain
        + '\''
        + ", userPoolId='"
        + userPoolId
        + '\''
        + ", username='"
        + username
        + '\''
        + ", federated="
        + federated
        + '}';
  }
}
