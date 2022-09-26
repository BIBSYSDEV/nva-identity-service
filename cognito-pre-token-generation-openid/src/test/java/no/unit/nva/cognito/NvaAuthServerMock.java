package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import java.net.HttpURLConnection;
import no.unit.nva.commons.json.JsonUtils;

public final class NvaAuthServerMock {
    
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final boolean MATCH_CASE = false;
    private final String clientId;
    private final String clientSecret;
    
    public NvaAuthServerMock() {
        this.clientId = randomString();
        this.clientSecret = randomString();
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public String setupCognitoMockResponse() {
        var responseBody = JsonUtils.dtoObjectMapper.createObjectNode();
        var accessToken = randomString();
        responseBody.put("access_token", accessToken);
        stubFor(post("/oauth2/token")
                    .withBasicAuth(clientId, clientSecret)
                    .withHeader(CONTENT_TYPE, wwwFormUrlEncoded())
                    .withRequestBody(new ContainsPattern("grant_type"))
                    .willReturn(aResponse()
                                    .withStatus(HttpURLConnection.HTTP_OK)
                                    .withJsonBody(responseBody)));
        return accessToken;
    }
    
    public String getJwtToken() {
        return null;
    }
    
    private static EqualToPattern wwwFormUrlEncoded() {
        return new EqualToPattern(APPLICATION_X_WWW_FORM_URLENCODED, MATCH_CASE);
    }
}
