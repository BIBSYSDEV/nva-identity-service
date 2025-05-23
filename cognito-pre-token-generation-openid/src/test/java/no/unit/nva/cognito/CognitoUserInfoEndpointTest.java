package no.unit.nva.cognito;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.FakeCognito;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static no.unit.nva.cognito.CognitoCommunicationHandler.AUTHORIZATION_HEADER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CognitoUserInfoEndpointTest {

    public static final FakeContext CONTEXT = new FakeContext();
    public static final Path DEMO_COGNITO_USER_INFO = Path.of("cognito", "cognito_user_info_response.json");
    private CognitoUserInfoEndpoint handler;
    private ByteArrayOutputStream outputStream;
    private FakeCognito cognito;

    @BeforeEach
    public void init() {
        this.outputStream = new ByteArrayOutputStream();
        this.cognito = new FakeCognito(randomString());
        this.handler = new CognitoUserInfoEndpoint(cognito, new Environment());
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

    @Test
    void shouldLimitAccessWhenNoTermsAccepted()
        throws IOException {
        var demoUserInfo = demoUserAttributes();
        demoUserInfo.remove(CognitoClaims.CUSTOMER_ACCEPTED_TERMS);
        var accessToken = addDemoUserInfoToFakeCognito(demoUserInfo);
        var request = requestWithAccessToken(accessToken);
        handler.handleRequest(request, outputStream, CONTEXT);
        var response = GatewayResponse.fromOutputStream(outputStream, Map.class);
        var responseAsMap = response.getBodyObject(Map.class);
        assertFalse(responseAsMap.containsKey(CognitoClaims.ACCESS_RIGHTS_CLAIM));
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

    private AttributeType toAttributeType(Entry<String, String> entry) {
        return AttributeType.builder().name(entry.getKey()).value(entry.getValue()).build();
    }

    private Map<String, String> demoUserAttributes() throws IOException {
        var demoContent = IoUtils.stringFromResources(DEMO_COGNITO_USER_INFO);
        var mapType = JsonConfig.getTypeFactory().constructMapType(Map.class, String.class, String.class);
        return JsonConfig.readValue(demoContent, mapType);
    }

    private InputStream requestWithAccessToken(String accessToken) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withHeaders(authHeader(accessToken))
                   .withUserName(randomString())
                   .build();
    }

    private Map<String, String> authHeader(String accessToken) {
        return Map.of(AUTHORIZATION_HEADER, "Bearer " + accessToken);
    }
}