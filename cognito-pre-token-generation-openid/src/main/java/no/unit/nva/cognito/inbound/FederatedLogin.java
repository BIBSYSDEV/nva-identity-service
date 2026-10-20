package no.unit.nva.cognito.inbound;

import static java.util.Objects.nonNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.PersonName;

/**
 * What the handler needs from an inbound federation event, read once so that the rest of the
 * handler never touches the raw event. The national identity number is the only sensitive value and
 * is wrapped in a type that masks itself when printed. Dataporten sends the number in the id token
 * for both Feide and ID-porten logins and, for Feide, also as a user info attribute.
 *
 * @param providerName the identity provider as configured in the user pool
 * @param username the Cognito username, which is the provider name and the provider's subject
 * @param nin the national identity number, null when the provider sent none
 * @param feideIdentifier the Feide identifier, null for logins without one
 * @param orgFeideDomain the Feide organization domain, null for logins without one
 * @param name the person's name as the provider sent it, null when it could not be read
 * @param idTokenKeys the claim names in the id token, for logging without values
 * @param userInfoKeys the attribute names in the user info, for logging without values
 */
record FederatedLogin(
    String providerName,
    String username,
    NationalIdentityNumber nin,
    String feideIdentifier,
    String orgFeideDomain,
    PersonName name,
    Set<String> idTokenKeys,
    Set<String> userInfoKeys) {

  static final String NIN_CLAIM = "https://n.feide.no/claims/nin";
  static final String FEIDE_NIN_ATTRIBUTE = "norEduPersonNIN";
  static final String PRINCIPAL_NAME_CLAIM = "https://n.feide.no/claims/eduPersonPrincipalName";
  static final String HOME_ORGANIZATION_ATTRIBUTE = "schacHomeOrganization";
  static final String GIVEN_NAME_ATTRIBUTE = "givenName";
  static final String SURNAME_ATTRIBUTE = "sn";
  static final String FULL_NAME_ATTRIBUTE = "name";
  private static final String USERNAME_SEPARATOR = "_";

  static FederatedLogin from(InboundFederationEvent event) {
    var request = event.getRequest();
    var idToken = request.getAttributes().getIdToken();
    var userInfo = request.getAttributes().getUserInfo();
    return new FederatedLogin(
        request.getProviderName(),
        usernameOf(request.getProviderName(), event.getUserName()),
        nationalIdentityNumber(idToken, NIN_CLAIM)
            .or(() -> nationalIdentityNumber(userInfo, FEIDE_NIN_ATTRIBUTE))
            .orElse(null),
        nonBlank(idToken, PRINCIPAL_NAME_CLAIM).orElse(null),
        nonBlank(userInfo, HOME_ORGANIZATION_ATTRIBUTE).orElse(null),
        extractName(idToken, userInfo).orElse(null),
        new TreeSet<>(idToken.keySet()),
        new TreeSet<>(userInfo.keySet()));
  }

  /** Whether the name the provider sent matches, ignoring case and surrounding space. */
  boolean hasName(String givenName, String familyName) {
    return nonNull(name) && name.matches(givenName, familyName);
  }

  /** Nothing but the username, so that a login can never be printed with what the provider sent. */
  @Override
  public String toString() {
    return "FederatedLogin[username=" + username + "]";
  }

  private static Optional<NationalIdentityNumber> nationalIdentityNumber(
      Map<String, Object> values, String key) {
    return nonBlank(values, key).flatMap(NationalIdentityNumber::fromDigits);
  }

  /** The event carries the provider's subject as username; Cognito prefixes the provider name. */
  private static String usernameOf(String providerName, String subject) {
    var prefix = providerName + USERNAME_SEPARATOR;
    return subject.startsWith(prefix) ? subject : prefix + subject;
  }

  /**
   * Feide sends the given name and the surname as separate attributes. ID-porten sends only the
   * full name, which is split as the pre-token Lambda splits the mapped name: the last word is the
   * surname.
   */
  private static Optional<PersonName> extractName(
      Map<String, Object> idToken, Map<String, Object> userInfo) {
    var givenName = nonBlank(userInfo, GIVEN_NAME_ATTRIBUTE);
    var surname = nonBlank(userInfo, SURNAME_ATTRIBUTE);
    if (givenName.isPresent() && surname.isPresent()) {
      return Optional.of(new PersonName(givenName.get(), surname.get()));
    }
    return nonBlank(userInfo, FULL_NAME_ATTRIBUTE)
        .or(() -> nonBlank(idToken, FULL_NAME_ATTRIBUTE))
        .flatMap(PersonName::fromFullName);
  }

  private static Optional<String> nonBlank(Map<String, Object> values, String key) {
    return values.get(key) instanceof String value && !value.isBlank()
        ? Optional.of(value.trim())
        : Optional.empty();
  }
}
