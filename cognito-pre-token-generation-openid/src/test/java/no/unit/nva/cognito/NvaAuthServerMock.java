package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import java.util.Map;
import no.unit.nva.FakeCognito;
import no.unit.nva.identityservice.json.JsonConfig;

public class NvaAuthServerMock {


    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String JWT_TOKEN_FIELD = "access_token";
    public static final boolean MATCH_CASE = false;
    private final FakeCognito cognitoClient;
    private final String clientSecret;
    private final String clientId;
    private final WireMockServer wiremockServer;
    private String jwtToken;

    public NvaAuthServerMock(WireMockServer wireMockServer, FakeCognito cognitoClient) {
        this.wiremockServer = wireMockServer;
        this.cognitoClient = cognitoClient;
        clientSecret = cognitoClient.getFakeClientSecret();
        clientId = cognitoClient.getFakeClientId();
        setupCognitoMockResponse();
    }

    public FakeCognito getCognitoClient() {
        return cognitoClient;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public WireMockServer getWiremockServer() {
        return wiremockServer;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    private void setupCognitoMockResponse() {
        jwtToken = randomString();
        stubFor(post("/oauth2/token")
                    .withBasicAuth(clientId, clientSecret)
                    .withHeader(CONTENT_TYPE, wwwFormUrlEndcoded())
                    .withRequestBody(new ContainsPattern("grant_type"))
                    .willReturn(createCognitoResponse(jwtToken)));
    }

    private ResponseDefinitionBuilder createCognitoResponse(String jwtToken) {
        var jsonMap = Map.of(JWT_TOKEN_FIELD, jwtToken);
        var responseBody = attempt(() -> JsonConfig.asString(jsonMap)).orElseThrow();
        return aResponse().withStatus(HTTP_OK).withBody(responseBody);
    }

    private EqualToPattern wwwFormUrlEndcoded() {
        return new EqualToPattern(APPLICATION_X_WWW_FORM_URLENCODED, MATCH_CASE);
    }
}
