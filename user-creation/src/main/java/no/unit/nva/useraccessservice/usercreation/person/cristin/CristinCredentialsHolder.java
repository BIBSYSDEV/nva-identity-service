package no.unit.nva.useraccessservice.usercreation.person.cristin;

import static java.util.Objects.isNull;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static nva.commons.core.attempt.Try.attempt;

import java.util.concurrent.atomic.AtomicReference;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceUnavailableException;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Cristin credentials, read from Secrets Manager once and kept until Cristin rejects them. A
 * Lambda instance can live for hours and the secret can be rotated meanwhile, so a rejected value
 * is read again rather than trusted.
 */
final class CristinCredentialsHolder {

  private static final Logger LOGGER = LoggerFactory.getLogger(CristinCredentialsHolder.class);
  private static final String COULD_NOT_READ_MESSAGE = "Could not read the Cristin credentials";
  private static final String READ_AHEAD_FAILED_MESSAGE =
      "Could not read the Cristin credentials ahead of time, reading them at the first call";

  private final SecretsReader secretsReader;
  private final AtomicReference<CristinCredentials> credentials = new AtomicReference<>();

  private CristinCredentialsHolder(SecretsReader secretsReader) {
    this.secretsReader = secretsReader;
  }

  /**
   * Reads the credentials right away, so the first call does not have to. A failed read is not an
   * error here: the first call reads them again, and fails only if that read fails too.
   */
  static CristinCredentialsHolder readAheadFrom(SecretsReader secretsReader) {
    var holder = new CristinCredentialsHolder(secretsReader);
    holder.readAhead();
    return holder;
  }

  private void readAhead() {
    var attemptedRead = attempt(this::current);
    if (attemptedRead.isFailure()) {
      LOGGER.warn(READ_AHEAD_FAILED_MESSAGE, attemptedRead.getException());
    }
  }

  /** Without its credentials the registry cannot be queried, as when it is unreachable. */
  CristinCredentials current() {
    var current = credentials.get();
    if (isNull(current)) {
      current = read();
      credentials.set(current);
    }
    return current;
  }

  CristinCredentials readAgain() {
    credentials.set(null);
    return current();
  }

  private CristinCredentials read() {
    return attempt(() ->
            secretsReader.fetchClassSecret(
                CRISTIN_CREDENTIALS_SECRET_NAME, CristinCredentials.class))
        .orElseThrow(failure ->
            new IdentityServiceUnavailableException(
                COULD_NOT_READ_MESSAGE, failure.getException()));
  }
}
