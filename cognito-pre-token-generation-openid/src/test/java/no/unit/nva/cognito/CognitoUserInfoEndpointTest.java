package no.unit.nva.cognito;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import no.unit.nva.FakeCognito;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

class CognitoUserInfoEndpointTest {

    public static final FakeContext CONTEXT = new FakeContext();
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final Path DEMO_COGNITO_USER_INFO = Path.of("cognito", "cognito_user_info_response.json");
    private CognitoUserInfoEndpoint handler;
    private ByteArrayOutputStream outputStream;
    private FakeCognito cognito;

    @BeforeEach
    public void init() {
        this.outputStream = new ByteArrayOutputStream();
        this.cognito = new FakeCognito(randomString());
        this.handler = new CognitoUserInfoEndpoint(cognito);
    }

    @Test
    void shouldReturnCognitoUserInfoWhenRequestingWithAccessTokenHavingTheOnlyAvailableScopeDuringE2ETests()
        throws IOException {
        var demoUserInfo = demoUserAttributes();
        var accessToken = addDemoUserInfoToFakeCognito(demoUserInfo);
        var request = requestWithAccessToken(accessToken);
        handler.handleRequest(request, outputStream, CONTEXT);
        var response = GatewayResponse.fromOutputStream(outputStream, Map.class);
        var responseAsMap = response.getBodyObject(Map.class);
        assertThat(responseAsMap, is(equalTo(demoUserInfo)));
    }

    private String addDemoUserInfoToFakeCognito(Map<String, String> demoUserInfo) {
        var accessToken = randomString();
        var userResponse = createUserResponseFromDemoUserInfo(demoUserInfo);
        cognito.addUser(accessToken, userResponse);
        return accessToken;
    }

    private GetUserResponse createUserResponseFromDemoUserInfo(Map<String, String> demoUserInfo) {
        var demoContent = demoUserInfo.entrySet()
            .stream()
            .map(this::toAttributeType)
            .collect(Collectors.toList());
        return GetUserResponse.builder().userAttributes(demoContent).build();
    }

    private Map<String, String> demoUserAttributes() throws IOException {
        var demoContent = IoUtils.stringFromResources(DEMO_COGNITO_USER_INFO);
        var mapType = JsonConfig.getTypeFactory().constructMapType(Map.class, String.class, String.class);
        return JsonConfig.readValue(demoContent, mapType);
    }

    private AttributeType toAttributeType(Entry<String, String> entry) {
        return AttributeType.builder().name(entry.getKey()).value(entry.getValue()).build();
    }

    private InputStream requestWithAccessToken(String accessToken) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withHeaders(authHeader(accessToken))
            .build();
    }

    private Map<String, String> authHeader(String accessToken) {
        return Map.of(AUTHORIZATION_HEADER, "Bearer " + accessToken);
    }
}