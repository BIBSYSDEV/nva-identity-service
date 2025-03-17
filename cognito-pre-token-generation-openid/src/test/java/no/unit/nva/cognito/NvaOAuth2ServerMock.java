package no.unit.nva.cognito;

import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import no.unit.nva.commons.json.JsonUtils;

import java.net.HttpURLConnection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

public final class NvaOAuth2ServerMock {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final boolean MATCH_CASE = false;
    private final String clientId;
    private final String clientSecret;

    public NvaOAuth2ServerMock() {
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

    private static EqualToPattern wwwFormUrlEncoded() {
        return new EqualToPattern(APPLICATION_X_WWW_FORM_URLENCODED, MATCH_CASE);
    }
}
