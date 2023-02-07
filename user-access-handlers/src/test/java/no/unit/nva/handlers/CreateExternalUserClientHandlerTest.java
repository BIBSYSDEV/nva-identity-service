package no.unit.nva.handlers;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.database.ExternalUserService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.model.CreateExternalUserResponse;
import no.unit.nva.useraccessservice.model.CreateExternalUserRequest;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfoConstants;
import nva.commons.core.Environment;
import nva.commons.logutils.LogUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

public class CreateExternalUserClientHandlerTest extends HandlerTest {

    public static final String CLIENT_NAME = "client1";
    public static final String CLIENT_ID= "id1";
    public static final String CLIENT_SECRET = "secret1";
    private static final String EXTERNAL_USER_POOL_URL = new Environment().readEnv("EXTERNAL_USER_POOL_URL");
    private CreateExternalUserClientHandler handler;
    private FakeContext context;
    private ByteArrayOutputStream outputStream;
    private CognitoIdentityProviderClient cognitoClient;

    @BeforeEach
    public void setup()  {
        cognitoClient = Mockito.mock(CognitoIdentityProviderClient.class);
        var externalUserService = new ExternalUserService(cognitoClient);

        var response = CreateUserPoolClientResponse.builder().userPoolClient(
            UserPoolClientType.builder().clientId(CLIENT_ID).clientSecret(CLIENT_SECRET).build()
        ).build();

        Mockito.when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenReturn(response);

        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
        handler = new CreateExternalUserClientHandler(externalUserService);
    }

    @Test
    public void shouldReturnCredentialsWhenClientDoesNotExist() throws IOException {
        var request = new CreateExternalUserRequest(CLIENT_NAME);
        var gatewayResponse = sendRequest(createBackendRequest(request), CreateExternalUserResponse.class);

        var cognitoCredentials = gatewayResponse.getBodyObject(CreateExternalUserResponse.class);

        assertThat(cognitoCredentials.getClientId(),  is(equalTo(CLIENT_ID)));
        assertThat(cognitoCredentials.getClientSecret(),  is(equalTo(CLIENT_SECRET)));
        assertThat(cognitoCredentials.getClientUrl(),  is(equalTo(EXTERNAL_USER_POOL_URL)));
    }

    @Test
    public void shouldNotExposeExceptionCausedByCognitoClient() throws IOException {
        var exceptionMsg = "some exception";
        Mockito.when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenThrow(SdkClientException.create(exceptionMsg));

        var request = new CreateExternalUserRequest(CLIENT_NAME);
        var gatewayResponse = sendRequest(createBackendRequest(request), CreateExternalUserResponse.class);
        assertThat(gatewayResponse.getBody(), not(containsString(exceptionMsg)));
    }

    @Test
    public void shouldLogErrorsCausedByCognitoClient() throws IOException {
        var exceptionMsg = "some exception";
        Mockito.when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenThrow(SdkClientException.create(exceptionMsg));

        var request = new CreateExternalUserRequest(CLIENT_NAME);
        var testAppender = LogUtils.getTestingAppenderForRootLogger();
        sendRequest(createBackendRequest(request), CreateExternalUserResponse.class);
        assertThat(testAppender.getMessages(), Matchers.containsString(exceptionMsg));
    }

    private InputStream createBackendRequest(CreateExternalUserRequest requestBody)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateExternalUserRequest>(dtoObjectMapper)
                   .withScope(RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE)
                   .withBody(requestBody)
                   .build();
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }
}