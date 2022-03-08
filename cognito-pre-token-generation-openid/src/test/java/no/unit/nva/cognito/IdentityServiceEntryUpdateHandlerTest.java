package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.cognito.NetworkingUtils.APPLICATION_X_WWW_FORM_URLENCODED;
import static no.unit.nva.cognito.NetworkingUtils.CONTENT_TYPE;
import static no.unit.nva.cognito.NetworkingUtils.JWT_TOKEN_FIELD;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class IdentityServiceEntryUpdateHandlerTest {

    public static final boolean MATCH_CASE = false;
    private final Context context = new FakeContext();

    private String clientSecret;
    private String clientId;
    private IdentityServiceEntryUpdateHandler handler;
    private String jwtToken;

    private WireMockServer httpServer;

    @BeforeEach
    public void init() {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        jwtToken = randomString();
        FakeCognitoIdentityProviderClient cognitoClient = new FakeCognitoIdentityProviderClient();
        clientSecret = cognitoClient.getFakeClientSecret();
        clientId = cognitoClient.getFakeClientId();
        setupCognitoMock();
        URI serverUri = URI.create(httpServer.baseUrl());
        HttpClient httpClient = WiremockHttpClient.create();
        handler = new IdentityServiceEntryUpdateHandler(cognitoClient, httpClient, serverUri);
    }

    @AfterEach
    public void close() {
        httpServer.stop();
    }

    @Test
    @Disabled("online test")
    void onlineTest() {
        var handler = new IdentityServiceEntryUpdateHandler();
        Context fakeContent = new FakeContext();
        CognitoUserPoolPreTokenGenerationEvent event = CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId("eu-west-1_fPzloej9T")
            .withCallerContext(CallerContext.builder()
                                   .withClientId("71qlmhqe47ovdb2m54l99d0r0t")
                                   .build())
            .build();
        handler.handleRequest(event, fakeContent);
    }

    @Test
    void shouldRetrieveCognitoBackendClientSecret() {
        TestAppender logger = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(randomEvent(), context);
        assertThat(logger.getMessages(), containsString(jwtToken));
    }

    private StubMapping setupCognitoMock() {
        return stubFor(post("/oauth2/token")
                           .withBasicAuth(clientId, clientSecret)
                           .withHeader(CONTENT_TYPE, expectedContentType())
                           .withRequestBody(new ContainsPattern("grant_type"))
                           .willReturn(aResponse().withStatus(HTTP_OK).withBody(responseBody())));
    }

    private String responseBody() {
        var jsonMap = Map.of(JWT_TOKEN_FIELD, jwtToken);
        return attempt(() -> JsonConfig.objectMapper.asString(jsonMap)).orElseThrow();
    }

    private EqualToPattern expectedContentType() {
        return new EqualToPattern(APPLICATION_X_WWW_FORM_URLENCODED, MATCH_CASE);
    }

    private CognitoUserPoolPreTokenGenerationEvent randomEvent() {
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withCallerContext(CallerContext.builder().withClientId(clientId).build())
            .build();
    }
}