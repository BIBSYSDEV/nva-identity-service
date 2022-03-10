package no.unit.nva.cognito;

import static no.unit.nva.cognito.EnvironmentVariables.AWS_REGION;
import static no.unit.nva.cognito.EnvironmentVariables.COGNITO_HOST;
import static no.unit.nva.cognito.NetworkingUtils.CRISTIN_HOST;
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
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
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

    @JacocoGenerated
    public IdentityServiceEntryUpdateHandler() {
        this(defaultCognitoClient(), HttpClient.newHttpClient(), defaultCognitoUri(), CRISTIN_HOST);
    }

    public IdentityServiceEntryUpdateHandler(CognitoIdentityProviderClient cognitoClient,
                                             HttpClient httpClient,
                                             URI cognitoHost,
                                             URI cristinHost) {

        this.requestAuthorizer = new RequestAuthorizer(cognitoClient, cognitoHost, httpClient);
        this.cristinClient = new CristinClient(cristinHost, httpClient);
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {

        var userAttributes = input.getRequest().getUserAttributes();
        var nin = extractNin(userAttributes);

        var jwtToken = requestAuthorizer.fetchJwtToken(input.getUserPoolId());
        var cristinResponse = attempt(() -> cristinClient.sendRequestToCristin(jwtToken, nin)).orElseThrow();
        var orgIdentifiers =
            cristinResponse.getAffiliations().stream().filter(CristinAffiliation::isActive)
            .map(CristinAffiliation::getOrganizationUri)
            .map(UriWrapper::new)
            .map(UriWrapper::getFilename);

        var customGroups=orgIdentifiers.map(identifier->identifier+":Creator").toArray(String[]::new);
        var overrideDetails = ClaimsOverrideDetails.builder()
            .withClaimsToAddOrOverride(Map.of("accessRights","olariaolara"))
            .withGroupOverrideDetails(GroupConfiguration.builder().withGroupsToOverride(customGroups).build())
            .build();
        input.setResponse(Response.builder().withClaimsOverrideDetails(overrideDetails).build());
        return input;
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
