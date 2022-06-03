package no.unit.nva.cognito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CognitoUserInfoEndpointTest {

    public static final FakeContext CONTEXT = new FakeContext();
    private CognitoUserInfoEndpoint handler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        this.outputStream = new ByteArrayOutputStream();
        this.handler = new CognitoUserInfoEndpoint();
    }

    @Test
    void shouldReturnObjectWithSameStructureAsOriginalCognitoUserInfoEndpoint() throws IOException {
        var demoContent = IoUtils.stringFromResources(Path.of("cognito", "cognito_user_info_response.json"));
        var mapType = JsonConfig.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> contentAsMap = JsonConfig.readValue(demoContent, mapType);
        var expectedKeys = contentAsMap.keySet();
        handler.handleRequest(emptyRequest(), outputStream, CONTEXT);
        var response = GatewayResponse.fromOutputStream(outputStream, Map.class);
        var responseBodyAsMap = response.getBodyObject(Map.class);
        var actualKeys = responseBodyAsMap.keySet();
        var diff = new HashSet<String>(expectedKeys);
        diff.removeAll(actualKeys);

        assertThat(diff.toString(), actualKeys, is(equalTo(expectedKeys)));
    }

    private InputStream emptyRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();
    }
}