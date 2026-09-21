package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import static java.util.function.Predicate.not;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static nva.commons.core.StringUtils.isBlank;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.AccessTokenGeneration;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.ClaimsAndScopeOverrideDetails;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2.Response;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.ClientDto;

public class M2MTokenEnricher {

  public static final String MISSING_CLIENT_ID_MESSAGE =
      "Pre token generation event has no client id";
  public static final String CLIENT_NOT_FOUND_MESSAGE = "Could not find stored client %s";
  public static final String INCOMPLETE_CLIENT_MESSAGE =
      "Stored client %s is missing required properties: %s";
  public static final String CUSTOMER_PROPERTY = "customer";
  public static final String CRISTIN_ORG_URI_PROPERTY = "cristinOrgUri";
  public static final String ACTING_USER_PROPERTY = "actingUser";

  private static final String PROPERTY_SEPARATOR = ", ";

  private final IdentityService identityService;

  public M2MTokenEnricher(IdentityService identityService) {
    this.identityService = identityService;
  }

  public CognitoUserPoolPreTokenGenerationEventV2 enrichAccessToken(
      CognitoUserPoolPreTokenGenerationEventV2 input) {
    var clientId = extractClientId(input);
    var client = getClientById(clientId);
    validateClient(client);
    var claims = createClaims(client);
    return createEnrichedTokenWithClaims(input, claims);
  }

  private static String extractClientId(CognitoUserPoolPreTokenGenerationEventV2 input) {
    return Optional.ofNullable(input.getCallerContext())
        .map(CallerContext::getClientId)
        .filter(not(String::isBlank))
        .orElseThrow(() -> new RuntimeException(MISSING_CLIENT_ID_MESSAGE));
  }

  private ClientDto getClientById(String clientId) {
    var queryObject = ClientDto.newBuilder().withClientId(clientId).build();
    return attempt(() -> identityService.getClient(queryObject))
        .orElseThrow(
            failure ->
                new RuntimeException(
                    CLIENT_NOT_FOUND_MESSAGE.formatted(clientId), failure.getException()));
  }

  private static Map<String, String> createClaims(ClientDto client) {
    var customer = client.getCustomer().toString();
    return Map.of(
        CURRENT_CUSTOMER_CLAIM,
        customer,
        ALLOWED_CUSTOMERS_CLAIM,
        customer,
        TOP_ORG_CRISTIN_ID,
        client.getCristinOrgUri().toString(),
        NVA_USERNAME_CLAIM,
        client.getActingUser());
  }

  private static void validateClient(ClientDto client) {
    var missingProperties = new ArrayList<String>();
    if (isNull(client.getCustomer())) {
      missingProperties.add(CUSTOMER_PROPERTY);
    }
    if (isNull(client.getCristinOrgUri())) {
      missingProperties.add(CRISTIN_ORG_URI_PROPERTY);
    }
    if (isBlank(client.getActingUser())) {
      missingProperties.add(ACTING_USER_PROPERTY);
    }
    if (!missingProperties.isEmpty()) {
      throw new RuntimeException(
          INCOMPLETE_CLIENT_MESSAGE.formatted(
              client.getClientId(), String.join(PROPERTY_SEPARATOR, missingProperties)));
    }
  }

  private static CognitoUserPoolPreTokenGenerationEventV2 createEnrichedTokenWithClaims(
      CognitoUserPoolPreTokenGenerationEventV2 input, Map<String, String> claims) {
    var accessTokenGeneration = createAccessTokenGenerationWith(claims);
    input.setResponse(createResponseWith(accessTokenGeneration));

    return input;
  }

  private static AccessTokenGeneration createAccessTokenGenerationWith(Map<String, String> claims) {
    return AccessTokenGeneration.builder().withClaimsToAddOrOverride(claims).build();
  }

  private static Response createResponseWith(AccessTokenGeneration accessTokenGeneration) {
    return Response.builder()
        .withClaimsAndScopeOverrideDetails(
            createClaimsAndScopeOverrideDetailsWith(accessTokenGeneration))
        .build();
  }

  private static ClaimsAndScopeOverrideDetails createClaimsAndScopeOverrideDetailsWith(
      AccessTokenGeneration accessTokenGeneration) {
    return ClaimsAndScopeOverrideDetails.builder()
        .withAccessTokenGeneration(accessTokenGeneration)
        .build();
  }
}
