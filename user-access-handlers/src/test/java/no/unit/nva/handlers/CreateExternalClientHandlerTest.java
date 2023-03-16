package no.unit.nva.handlers;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.handlers.CreateExternalClientHandler.MISSING_CLIENT_NAME;
import static no.unit.nva.handlers.CreateExternalClientHandler.MISSING_CUSTOMER;
import static no.unit.nva.handlers.CreateExternalClientHandler.MISSING_SCOPES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Client;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import no.unit.nva.CognitoService;
import no.unit.nva.database.ExternalClientService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.CreateExternalClientRequest;
import no.unit.nva.useraccessservice.model.CreateExternalClientResponse;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfoConstants;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.logutils.LogUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.zalando.problem.Problem;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeResourceServerRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeResourceServerResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceServerScopeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceServerType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ScopeDoesNotExistException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

public class CreateExternalClientHandlerTest extends HandlerTest {

    public static final String CLIENT_NAME = "client1";
    public static final String CLIENT_ID = "id1";
    public static final String CLIENT_SECRET = "secret1";
    public static final String INVALID_SCOPE = "https://scopes/invalid-scope";
    private static final String EXTERNAL_USER_POOL_URL = new Environment().readEnv("EXTERNAL_USER_POOL_URL");
    public static final String EXTERNAL_SCOPE_IDENTIFIER = new Environment().readEnv("EXTERNAL_SCOPE_IDENTIFIER");
    private CreateExternalClientHandler handler;
    private FakeContext context;
    private ByteArrayOutputStream outputStream;
    private CognitoIdentityProviderClient cognitoClient;

    @BeforeEach
    public void setup() {
        this.cognitoClient = setupCognitoMock();
        databaseService = createDatabaseServiceUsingLocalStorage();
        var cognitoService = new CognitoService(cognitoClient);

        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
        handler = new CreateExternalClientHandler(databaseService, cognitoService);
    }

    private CognitoIdentityProviderClient setupCognitoMock() {
        var mock = Mockito.mock(CognitoIdentityProviderClient.class);

        var resourceServerResponse = DescribeResourceServerResponse.builder().resourceServer(
            ResourceServerType.builder().scopes(
                ResourceServerScopeType.builder().scopeName("publication-read").build(),
                ResourceServerScopeType.builder().scopeName("publication-upsert").build()
            ).build()
        ).build();
        when(mock.describeResourceServer(any(DescribeResourceServerRequest.class))).thenReturn(
            resourceServerResponse
        );

        when(mock.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenAnswer((Answer) invocation -> {
                Object[] args = invocation.getArguments();
                CreateUserPoolClientRequest request = (CreateUserPoolClientRequest) args[0];

                if (request.allowedOAuthScopes().stream().anyMatch(it -> it.equals(INVALID_SCOPE))) {
                    throw ScopeDoesNotExistException.builder().build();
                }

                var userPoolClient = UserPoolClientType
                                         .builder()
                                         .clientId(CLIENT_ID)
                                         .clientSecret(CLIENT_SECRET)
                                         .allowedOAuthScopes(request.allowedOAuthScopes())
                                         .build();

                var response = CreateUserPoolClientResponse
                                   .builder()
                                   .userPoolClient(
                                       userPoolClient
                                   )
                                   .build();
                return response;
            });

        return mock;
    }

    @Test
    public void shouldReturnCredentialsWhenClientDoesNotAlreadyExist() throws IOException, URISyntaxException {
        var request = new CreateExternalClientRequest(CLIENT_NAME, new URI("https://example.org/123"), List.of());
        var gatewayResponse = sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);

        var cognitoCredentials = gatewayResponse.getBodyObject(CreateExternalClientResponse.class);

        assertThat(cognitoCredentials.getClientId(), is(equalTo(CLIENT_ID)));
        assertThat(cognitoCredentials.getClientSecret(), is(equalTo(CLIENT_SECRET)));
        assertThat(cognitoCredentials.getClientUrl(), is(equalTo(EXTERNAL_USER_POOL_URL)));
    }

    @Test
    public void clientShouldBeStoredToDatabase() throws IOException, URISyntaxException, NotFoundException {
        var customer = RandomDataGenerator.randomUri();
        var request = new CreateExternalClientRequest(CLIENT_NAME, customer, List.of());
        sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);

        var expected = ClientDto
                           .newBuilder()
                           .withClientId(CLIENT_ID)
                           .withCustomer(customer)
                           .build();

        var found = databaseService.getClient(expected);

        assertThat(found.getClientId(), is(equalTo(CLIENT_ID)));
        assertThat(found.getCustomer(), is(equalTo(customer)));
    }

    @Test
    public void shouldReturnSameCustomerThatWasRequested() throws IOException, URISyntaxException {
        var customer = randomCristinOrgId();
        var request = new CreateExternalClientRequest(CLIENT_NAME, customer, List.of());
        var gatewayResponse = sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);

        var response = gatewayResponse.getBodyObject(CreateExternalClientResponse.class);

        assertThat(response.getCustomer(), is(equalTo(customer)));
    }

    @Test
    public void shouldReturnSameScopeThatWasRequested() throws IOException, URISyntaxException {
        var scopes = List.of(
            "https://api.nva.unit.no/scopes/third-party/publication-read",
            "https://api.nva.unit.no/scopes/third-party/publication-upsert"
        );

        var request = new CreateExternalClientRequest(CLIENT_NAME, new URI("https://example.org/123"), scopes);
        var gatewayResponse = sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);

        var response = gatewayResponse.getBodyObject(CreateExternalClientResponse.class);

        assertThat(response.getScopes(), is(equalTo(scopes)));
    }

    @Test
    public void shouldInformCallerOfInvalidRequestedScopes() throws IOException, URISyntaxException {
        var validScope = EXTERNAL_SCOPE_IDENTIFIER + "/publication-read";
        var scopes = List.of(
            validScope,
            INVALID_SCOPE
        );

        var request = new CreateExternalClientRequest(CLIENT_NAME, new URI("https://example.org/123"), scopes);
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(INVALID_SCOPE));
        assertThat(response.getDetail(), not(containsString(validScope)));
    }

    @Test
    public void shouldReturnBadRequestWhenScopeIsMissingInRequest() throws IOException, URISyntaxException {
        var request = new CreateExternalClientRequest(CLIENT_NAME, new URI("https://example.org/123"), null);
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(MISSING_SCOPES));
    }

    @Test
    public void shouldReturnBadRequestWhenCustomerIsMissingInRequest() throws IOException, URISyntaxException {
        var request = new CreateExternalClientRequest(CLIENT_NAME, null, List.of());
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(MISSING_CUSTOMER));
    }

    @Test
    public void shouldReturnBadRequestWhenClientNameIsMissingInRequest() throws IOException, URISyntaxException {
        var request = new CreateExternalClientRequest(null, new URI("https://example.org/123"), List.of());
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(MISSING_CLIENT_NAME));
    }

    @Test
    public void shouldReturnDetectMultipleIssuesInRequest() throws IOException {
        var request = new CreateExternalClientRequest(null, null, null);
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(MISSING_SCOPES));
        assertThat(response.getDetail(), containsString(MISSING_CLIENT_NAME));
        assertThat(response.getDetail(), containsString(MISSING_CUSTOMER));
        assertThat(response.getDetail(), not(containsString("[")));
    }

    @Test
    public void shouldNotExposeExceptionCausedByCognitoClient() throws IOException, URISyntaxException {
        var exceptionMsg = "some exception";
        when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenThrow(SdkClientException.create(exceptionMsg));

        var request = new CreateExternalClientRequest(CLIENT_NAME, new URI("https://example.org/123"), List.of());
        var gatewayResponse = sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);
        assertThat(gatewayResponse.getBody(), not(containsString(exceptionMsg)));
    }

    @Test
    public void shouldLogErrorsCausedByCognitoClient() throws IOException, URISyntaxException {
        var exceptionMsg = "some exception";
        when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenThrow(SdkClientException.create(exceptionMsg));

        var request = createRequest(List.of());
        var testAppender = LogUtils.getTestingAppenderForRootLogger();
        sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);
        assertThat(testAppender.getMessages(), Matchers.containsString(exceptionMsg));
    }

    private CreateExternalClientRequest createRequest(List<String> scopes) throws URISyntaxException {
        return new CreateExternalClientRequest(CLIENT_NAME, new URI("https://example.org/123"), scopes);
    }

    private InputStream createBackendRequest(CreateExternalClientRequest requestBody)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateExternalClientRequest>(dtoObjectMapper)
                   .withScope(RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE)
                   .withBody(requestBody)
                   .build();
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }
}