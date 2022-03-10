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
import static no.unit.nva.cognito.NetworkingUtils.AUTHORIZATION_HEADER;
import static no.unit.nva.cognito.NetworkingUtils.CONTENT_TYPE;
import static no.unit.nva.cognito.NetworkingUtils.JWT_TOKEN_FIELD;
import static no.unit.nva.cognito.cristin.CristinClient.REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Request;
import com.fasterxml.jackson.jr.ob.JSON;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.ioutils.IoUtils;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupRequest;

class IdentityServiceEntryUpdateHandlerTest {

    public static final boolean MATCH_CASE = false;
    public static final boolean IGNORE_ARRAY_ORDER = true;
    public static final boolean DO_NOT_IGNORE_OTHER_ELEMENTS = false;
    public static final String RANDOM_NIN = randomString();
    private final Context context = new FakeContext();

    private static final FakeCognitoIdentityProviderClient COGNITO_CLIENT = new FakeCognitoIdentityProviderClient();
    private static final String CLIENT_SECRET = COGNITO_CLIENT.getFakeClientSecret();
    private static final String CLIENT_ID = COGNITO_CLIENT.getFakeClientId();
    private IdentityServiceEntryUpdateHandler handler;
    private String jwtToken;

    private WireMockServer httpServer;
    private HttpClient httpClient;
    private URI serverUri;

    public static Stream<CognitoUserPoolPreTokenGenerationEvent> eventProvider() {
        return Stream.of(randomEventOfFeideUser(), randomEventOfNonFeideUser());
    }

    @BeforeEach
    public void init() {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        jwtToken = randomString();
        serverUri = URI.create(httpServer.baseUrl());
        httpClient = WiremockHttpClient.create();

        setupCognitoMock();
        setupCristinServiceMock();
        handler = new IdentityServiceEntryUpdateHandler(COGNITO_CLIENT, httpClient, serverUri, serverUri);
    }

    @AfterEach
    public void close() {
        httpServer.stop();
    }


    @ParameterizedTest(name = "should send request to Cristin Service to read person by NIN")
    @MethodSource("eventProvider")
    void shouldSendRequestToCristinServiceToReadPersonByNin(CognitoUserPoolPreTokenGenerationEvent event) {
        assertDoesNotThrow(() -> handler.handleRequest(event, context));
    }

    @ParameterizedTest(name = "should create UserGroup when UserGroup does not exist")
    @MethodSource("eventProvider")
    void shouldCreateUserGroupWhenUserGroupDoesNotExist(CognitoUserPoolPreTokenGenerationEvent event) {
        var newEvent=handler.handleRequest(event,context);
        var groupsToOverride=
            newEvent.getResponse().getClaimsOverrideDetails().getGroupOverrideDetails().getGroupsToOverride();
        assertThat(Arrays.asList(groupsToOverride), contains("groupA", "groupB"));
    }

    private void setupCognitoMock() {
        stubFor(post("/oauth2/token")
                    .withBasicAuth(CLIENT_ID, CLIENT_SECRET)
                    .withHeader(CONTENT_TYPE, wwwFormUrlEndcoded())
                    .withRequestBody(new ContainsPattern("grant_type"))
                    .willReturn(aResponse().withStatus(HTTP_OK).withBody(cognitoResponseBody())));
    }

    private void setupCristinServiceMock() {
        stubFor(post("/person/identityNumber")
                    .withHeader(AUTHORIZATION_HEADER, new EqualToPattern("Bearer " + jwtToken, MATCH_CASE))
                    .withHeader(CONTENT_TYPE, applicationJson())
                    .withRequestBody(cristinServiceRequestBody())
                    .willReturn(aResponse().withStatus(HTTP_OK).withBody(cristinResponseBody())));
    }

    private String cristinResponseBody() {
        return IoUtils.stringFromResources(Path.of("cristin_response_example.json"));
    }

    private ContentPattern<?> cristinServiceRequestBody() {
        String jsonBody = String.format(REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, RANDOM_NIN);
        return new EqualToJsonPattern(jsonBody, IGNORE_ARRAY_ORDER, DO_NOT_IGNORE_OTHER_ELEMENTS);
    }

    private StringValuePattern applicationJson() {
        return new EqualToPattern("application/json", MATCH_CASE);
    }

    private String cognitoResponseBody() {
        var jsonMap = Map.of(JWT_TOKEN_FIELD, jwtToken);
        return attempt(() -> JSON.std.asString(jsonMap)).orElseThrow();
    }

    private EqualToPattern wwwFormUrlEndcoded() {
        return new EqualToPattern(APPLICATION_X_WWW_FORM_URLENCODED, MATCH_CASE);
    }

    private static CognitoUserPoolPreTokenGenerationEvent randomEventOfFeideUser() {

        Map<String, String> userAttributes = Map.of(FEIDE_ID, randomString(), NIN_FOR_FEIDE_USERS, RANDOM_NIN);
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withUserName(randomString())
            .withRequest(Request.builder().withUserAttributes(userAttributes).build())
            .withCallerContext(CallerContext.builder().withClientId(CLIENT_ID).build())
            .build();
    }

    private static CognitoUserPoolPreTokenGenerationEvent randomEventOfNonFeideUser() {
        Map<String, String> userAttributes = Map.of(NIN_FON_NON_FEIDE_USERS, RANDOM_NIN);
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withUserName(randomString())
            .withRequest(Request.builder().withUserAttributes(userAttributes).build())
            .withCallerContext(CallerContext.builder().withClientId(CLIENT_ID).build())
            .build();
    }
}