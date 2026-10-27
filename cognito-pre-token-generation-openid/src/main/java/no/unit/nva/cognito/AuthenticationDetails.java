package no.unit.nva.cognito;

import java.util.Optional;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import nva.commons.core.JacocoGenerated;

public class AuthenticationDetails {
  private final NationalIdentityNumber nin;
  private final String feideIdentifier;
  private final String feideDomain;
  private final String userPoolId;
  private final String username;
  private final boolean federated;

  public AuthenticationDetails(
      NationalIdentityNumber nin,
      String feideIdentifier,
      String feideDomain,
      String userPoolId,
      String username,
      boolean federated) {
    this.nin = nin;
    this.feideIdentifier = feideIdentifier;
    this.feideDomain = feideDomain;
    this.userPoolId = userPoolId;
    this.username = username;
    this.federated = federated;
  }

  /** The national identity number, present only when the identity provider delivered one. */
  public Optional<NationalIdentityNumber> getNin() {
    return Optional.ofNullable(nin);
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
        nin, link.feideIdentifier(), link.orgFeideDomain(), userPoolId, username, federated);
  }

  @JacocoGenerated
  @Override
  public String toString() {
    return "AuthenticationDetails{"
        + "hasNin="
        + (nin != null)
        + ", feideIdentifier='"
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
