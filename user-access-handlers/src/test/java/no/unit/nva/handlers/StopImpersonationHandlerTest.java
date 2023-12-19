package no.unit.nva.handlers;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.handlers.SetImpersonationHandler.IMPERSONATION;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.ADMINISTRATE_APPLICATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import no.unit.nva.FakeCognito;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

class StopImpersonationHandlerTest {
    public static final String USERNAME = "username";
    private FakeCognito cognitoClient;
    private StopImpersonationHandler handler;
    private final Context context = new FakeContext();
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void setup() {
        cognitoClient = new FakeCognito(randomString());
        handler = new StopImpersonationHandler(cognitoClient);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void shouldReturn200ok() throws IOException {
        var request = createDefaultRequestForStopImpersonation(randomString());
        handler.handleRequest(request, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    @Test
    public void shouldCallCognitoAdminApiWithUsernameOfUser()
        throws IOException {
        var username = randomString();
        var request = createDefaultRequestForStopImpersonation(username);
        handler.handleRequest(request, outputStream, context);
        var setUsername = extractAdminUpdateRequestUsername();
        assertThat(setUsername, is(equalTo(username)));
    }

    @Test
    public void shouldCallCogntioAdminApiWithEmptyStringImpersonation()
        throws IOException {
        var request = createDefaultRequestForStopImpersonation(randomString());
        handler.handleRequest(request, outputStream, context);
        var setImpersonationClaim = extractAdminUpdateRequestUserAttribute(IMPERSONATION).get();
        assertThat(setImpersonationClaim.value(), is(equalTo("")));
    }

    private InputStream createDefaultRequestForStopImpersonation(String username) throws JsonProcessingException {
        var customer = randomUri();
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withCurrentCustomer(customer)
                   .withAccessRights(customer, ADMINISTRATE_APPLICATION)
                   .withAuthorizerClaim(USERNAME, username)
                   .withIssuer(randomString())
                   .build();
    }

    private Optional<AttributeType> extractAdminUpdateRequestUserAttribute(String userAttribute) {
        return cognitoClient.getAdminUpdateUserRequest()
                   .userAttributes().stream()
                   .filter(attribute -> attribute.name().equals(userAttribute))
                   .findFirst();
    }

    private String extractAdminUpdateRequestUsername() {
        return cognitoClient.getAdminUpdateUserRequest().username();
    }
}