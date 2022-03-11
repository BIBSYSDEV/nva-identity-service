package no.unit.nva.cognito;

import static no.unit.nva.cognito.EnvironmentVariables.AWS_REGION;
import static no.unit.nva.cognito.EnvironmentVariables.COGNITO_HOST;
import static no.unit.nva.cognito.NetworkingUtils.CRISTIN_HOST;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.ClaimsOverrideDetails;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.GroupConfiguration;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.cognito.cristin.CristinAffiliation;
import no.unit.nva.cognito.cristin.CristinClient;
import no.unit.nva.cognito.cristin.CristinResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public class IdentityServiceEntryUpdateHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    public static final String NIN_FOR_FEIDE_USERS = "custom:feideidnin";
    public static final String NIN_FON_NON_FEIDE_USERS = "custom:nin";
    public static final String FEIDE_ID = "custom:feideid";

    private final RequestAuthorizer requestAuthorizer;
    private final CristinClient cristinClient;
    private final CustomerService customerService;

    @JacocoGenerated
    public IdentityServiceEntryUpdateHandler() {
        this(defaultCognitoClient(), HttpClient.newHttpClient(), defaultCognitoUri(), CRISTIN_HOST,
             defaultCustomerService());
    }

    public IdentityServiceEntryUpdateHandler(CognitoIdentityProviderClient cognitoClient,
                                             HttpClient httpClient,
                                             URI cognitoHost,
                                             URI cristinHost,
                                             CustomerService customerService) {

        this.requestAuthorizer = new RequestAuthorizer(cognitoClient, cognitoHost, httpClient);
        this.cristinClient = new CristinClient(cristinHost, httpClient);
        this.customerService = customerService;
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {

        var userAttributes = input.getRequest().getUserAttributes();
        var nin = extractNin(userAttributes);

        CristinResponse cristinResponse = fetchPersonInformationFromCristin(input, nin);

        var customGroups = createCustomGroups(cristinResponse);

        var overrideDetails = ClaimsOverrideDetails.builder()
            .withGroupOverrideDetails(GroupConfiguration.builder().withGroupsToOverride(customGroups).build())
            .build();
        input.setResponse(Response.builder().withClaimsOverrideDetails(overrideDetails).build());
        return input;
    }

    private String[] createCustomGroups(CristinResponse cristinResponse) {
        return cristinResponse.getAffiliations().stream()
            .filter(CristinAffiliation::isActive)
            .map(CristinAffiliation::getOrganizationUri)
            .map(uri->customerService.getCustomerByCristinId(uri.toString()))
            .map(CustomerDto::getId)
            .map(URI::toString)
            .toArray(String[]::new);
    }

    private CristinResponse fetchPersonInformationFromCristin(CognitoUserPoolPreTokenGenerationEvent input, String nin) {
        var jwtToken = requestAuthorizer.fetchJwtToken(input.getUserPoolId());
        return attempt(() -> cristinClient.sendRequestToCristin(jwtToken, nin)).orElseThrow();
    }

    @JacocoGenerated
    private static URI defaultCognitoUri() {
        try {
            return new URI("https", COGNITO_HOST, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @JacocoGenerated
    private static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(AWS_REGION)
            .build();
    }

    private String extractNin(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(NIN_FOR_FEIDE_USERS))
            .or(() -> Optional.ofNullable(userAttributes.get(NIN_FON_NON_FEIDE_USERS)))
            .orElseThrow();
    }
}
