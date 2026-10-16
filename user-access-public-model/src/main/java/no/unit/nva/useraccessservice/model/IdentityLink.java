package no.unit.nva.useraccessservice.model;

import java.net.URI;
import java.time.Instant;

/**
 * The link from a Cognito user to the person it represents. Keyed on the Cognito username, which is
 * stable across logins, and rewritten on every login that resolves the person.
 *
 * @param username the Cognito username, for federated users providerName_sub
 * @param cristinId the person identifier the account resolved to
 * @param feideIdentifier the Feide identifier of the account, null for accounts without one
 * @param orgFeideDomain the Feide organization domain of the account, null for accounts without one
 * @param modified when the link was last written
 */
public record IdentityLink(
    String username,
    URI cristinId,
    String feideIdentifier,
    String orgFeideDomain,
    Instant modified) {}
