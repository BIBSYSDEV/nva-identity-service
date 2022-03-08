package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.FEIDE_ID;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.NIN_FON_NON_FEIDE_USERS;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.NIN_FOR_FEIDE_USERS;
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
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Request;
import com.fasterxml.jackson.jr.ob.JSON;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.stubs.FakeContext;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class IdentityServiceEntryUpdateHandlerTest {

    public static final boolean MATCH_CASE = false;
    private final Context context = new FakeContext();

    private static final FakeCognitoIdentityProviderClient COGNITO_CLIENT = new FakeCognitoIdentityProviderClient();
    private static final String CLIENT_SECRET = COGNITO_CLIENT.getFakeClientSecret();
    private static final String CLIENT_ID = COGNITO_CLIENT.getFakeClientId();
    private IdentityServiceEntryUpdateHandler handler;
    private String jwtToken;

    private WireMockServer httpServer;

    public static Stream<CognitoUserPoolPreTokenGenerationEvent> eventProvider() {
        return Stream.of(randomEventOfFeideUser(),randomEventOfNonFeideUser());
    }

    @BeforeEach
    public void init() {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        jwtToken = randomString();
        setupCognitoMock();
        URI serverUri = URI.create(httpServer.baseUrl());
        HttpClient httpClient = WiremockHttpClient.create();
        handler = new IdentityServiceEntryUpdateHandler(COGNITO_CLIENT, httpClient, serverUri);
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

    @ParameterizedTest(name = "should retrieve Congito backend secret client")
    @MethodSource("eventProvider")
    void shouldRetrieveCognitoBackendClientSecret(CognitoUserPoolPreTokenGenerationEvent event) {
        TestAppender logger = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(event, context);
        assertThat(logger.getMessages(), containsString(jwtToken));
    }

    private void setupCognitoMock() {
         stubFor(post("/oauth2/token")
                           .withBasicAuth(CLIENT_ID, CLIENT_SECRET)
                           .withHeader(CONTENT_TYPE, expectedContentType())
                           .withRequestBody(new ContainsPattern("grant_type"))
                           .willReturn(aResponse().withStatus(HTTP_OK).withBody(responseBody())));
    }

    private String responseBody() {
        var jsonMap = Map.of(JWT_TOKEN_FIELD, jwtToken);
        return attempt(() -> JSON.std.asString(jsonMap)).orElseThrow();
    }

    private EqualToPattern expectedContentType() {
        return new EqualToPattern(APPLICATION_X_WWW_FORM_URLENCODED, MATCH_CASE);
    }

    private static CognitoUserPoolPreTokenGenerationEvent randomEventOfFeideUser() {
        Map<String, String> userAttributes = Map.of(FEIDE_ID, randomString(), NIN_FOR_FEIDE_USERS, randomString());
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withRequest(Request.builder().withUserAttributes(userAttributes).build())
            .withCallerContext(CallerContext.builder().withClientId(CLIENT_ID).build())
            .build();
    }

    private static CognitoUserPoolPreTokenGenerationEvent randomEventOfNonFeideUser() {
        Map<String, String> userAttributes = Map.of(NIN_FON_NON_FEIDE_USERS, randomString());
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withRequest(Request.builder().withUserAttributes(userAttributes).build())
            .withCallerContext(CallerContext.builder().withClientId(CLIENT_ID).build())
            .build();
    }
}