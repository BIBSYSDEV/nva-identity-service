package no.unit.nva.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.CognitoService;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.handlers.CreateExternalClientHandler.MISSING_ACTING_USER;
import static no.unit.nva.handlers.CreateExternalClientHandler.MISSING_CLIENT_NAME;
import static no.unit.nva.handlers.CreateExternalClientHandler.MISSING_CRISTIN_ORG_URI;
import static no.unit.nva.handlers.CreateExternalClientHandler.MISSING_CUSTOMER_URI;
import static no.unit.nva.handlers.CreateExternalClientHandler.MISSING_SCOPES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CreateExternalClientHandlerTest extends HandlerTest {

    public static final String CLIENT_NAME = "client1";
    public static final String CLIENT_ID = "id1";
    public static final String CLIENT_SECRET = "secret1";
    public static final String INVALID_SCOPE = "https://scopes/invalid-scope";
    public static final String EXTERNAL_SCOPE_IDENTIFIER = new Environment().readEnv("EXTERNAL_SCOPE_IDENTIFIER");
    public static final URI SAMPLE_URI = URI.create("https://example.org/123");
    public static final String SAMPLE_ACTING_USER = "user@123";
    private static final String EXTERNAL_USER_POOL_URL = new Environment().readEnv("EXTERNAL_USER_POOL_URL");
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
    public void shouldReturnCredentialsWhenClientDoesNotAlreadyExist() throws IOException {
        var request = CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCustomerUri(SAMPLE_URI)
            .withCristinOrgUri(SAMPLE_URI)
            .withActingUser(SAMPLE_ACTING_USER)
            .withScopes(List.of())
            .build();
        var gatewayResponse = sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);

        var cognitoCredentials = gatewayResponse.getBodyObject(CreateExternalClientResponse.class);

        assertThat(cognitoCredentials.getClientId(), is(equalTo(CLIENT_ID)));
        assertThat(cognitoCredentials.getClientSecret(), is(equalTo(CLIENT_SECRET)));
        assertThat(cognitoCredentials.getClientUrl(), is(equalTo(EXTERNAL_USER_POOL_URL)));
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

    @Test
    public void clientShouldBeStoredToDatabase() throws IOException, NotFoundException {
        var customer = RandomDataGenerator.randomUri();
        var request = CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCustomerUri(customer)
            .withCristinOrgUri(SAMPLE_URI)
            .withActingUser(SAMPLE_ACTING_USER)
            .withScopes(List.of())
            .build();
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
        var request = CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCustomerUri(customer)
            .withCristinOrgUri(new URI("https://example.org/123"))
            .withActingUser(SAMPLE_ACTING_USER)
            .withScopes(List.of())
            .build();
        var gatewayResponse = sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);

        var response = gatewayResponse.getBodyObject(CreateExternalClientResponse.class);

        assertThat(response.getCustomer(), is(equalTo(customer)));
    }

    @Test
    public void shouldReturnSameScopeThatWasRequested() throws IOException {
        var scopes = List.of(
            "https://api.nva.unit.no/scopes/third-party/publication-read",
            "https://api.nva.unit.no/scopes/third-party/publication-upsert"
        );

        var request = CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCustomerUri(SAMPLE_URI)
            .withCristinOrgUri(SAMPLE_URI)
            .withActingUser(SAMPLE_ACTING_USER)
            .withScopes(scopes)
            .build();
        var gatewayResponse = sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);

        var response = gatewayResponse.getBodyObject(CreateExternalClientResponse.class);

        assertThat(response.getScopes(), is(equalTo(scopes)));
    }

    @Test
    public void shouldInformCallerOfInvalidRequestedScopes() throws IOException {
        var validScope = EXTERNAL_SCOPE_IDENTIFIER + "/publication-read";
        var scopes = List.of(
            validScope,
            INVALID_SCOPE
        );

        var request = CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCustomerUri(SAMPLE_URI)
            .withCristinOrgUri(SAMPLE_URI)
            .withActingUser(SAMPLE_ACTING_USER)
            .withScopes(scopes)
            .build();
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(INVALID_SCOPE));
        assertThat(response.getDetail(), not(containsString(validScope)));
    }

    @Test
    public void shouldReturnBadRequestWhenScopeIsMissingInRequest() throws IOException, URISyntaxException {
        var request = CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCustomerUri(new URI("https://example.org/123"))
            .withCristinOrgUri(new URI("https://example.org/123"))
            .withActingUser(SAMPLE_ACTING_USER)
            .build();
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(MISSING_SCOPES));
    }

    @Test
    public void shouldReturnBadRequestWhenCustomerIsMissingInRequest() throws IOException, URISyntaxException {
        var request = CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCristinOrgUri(new URI("https://example.org/123"))
            .withActingUser(SAMPLE_ACTING_USER)
            .withScopes(List.of())
            .build();
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(MISSING_CUSTOMER_URI));
    }

    @Test
    public void shouldReturnBadRequestWhenCristinIsMissingInRequest() throws IOException {
        var request = CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCustomerUri(SAMPLE_URI)
            .withActingUser(SAMPLE_ACTING_USER)
            .withScopes(List.of())
            .build();
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(MISSING_CRISTIN_ORG_URI));
    }

    @Test
    public void shouldReturnBadRequestWhenActingUserIsMissingInRequest() throws IOException {
        var request = CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCristinOrgUri(SAMPLE_URI)
            .withCustomerUri(SAMPLE_URI)
            .withScopes(List.of())
            .build();
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(MISSING_ACTING_USER));
    }

    @Test
    public void shouldReturnBadRequestWhenClientNameIsMissingInRequest() throws IOException {
        var request = CreateExternalClientRequest.newBuilder()
            .withCustomerUri(SAMPLE_URI)
            .withCristinOrgUri(SAMPLE_URI)
            .withActingUser(SAMPLE_ACTING_USER)
            .withScopes(List.of())
            .build();
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(MISSING_CLIENT_NAME));
    }

    @Test
    public void shouldReturnDetectMultipleIssuesInRequest() throws IOException {
        var request = new CreateExternalClientRequest();
        var gatewayResponse = sendRequest(createBackendRequest(request), Problem.class);

        var response = gatewayResponse.getBodyObject(Problem.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getDetail(), containsString(MISSING_SCOPES));
        assertThat(response.getDetail(), containsString(MISSING_CLIENT_NAME));
        assertThat(response.getDetail(), containsString(MISSING_CUSTOMER_URI));
        assertThat(response.getDetail(), containsString(MISSING_CRISTIN_ORG_URI));
        assertThat(response.getDetail(), containsString(MISSING_ACTING_USER));
        assertThat(response.getDetail(), not(containsString("[")));
    }

    @Test
    public void shouldNotExposeExceptionCausedByCognitoClient() throws IOException {
        var exceptionMsg = "some exception";
        when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenThrow(SdkClientException.create(exceptionMsg));

        var request = CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCustomerUri(SAMPLE_URI)
            .withCristinOrgUri(SAMPLE_URI)
            .withActingUser(SAMPLE_ACTING_USER)
            .withScopes(List.of())
            .build();

        var gatewayResponse = sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);
        assertThat(gatewayResponse.getBody(), not(containsString(exceptionMsg)));
    }

    @Test
    public void shouldLogErrorsCausedByCognitoClient() throws IOException {
        var exceptionMsg = "some exception";
        when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenThrow(SdkClientException.create(exceptionMsg));

        var request = createRequestForScopes(List.of());
        var testAppender = LogUtils.getTestingAppenderForRootLogger();
        sendRequest(createBackendRequest(request), CreateExternalClientResponse.class);
        assertThat(testAppender.getMessages(), Matchers.containsString(exceptionMsg));
    }

    private CreateExternalClientRequest createRequestForScopes(List<String> scopes) {
        return CreateExternalClientRequest.newBuilder()
            .withClientName(CLIENT_NAME)
            .withCustomerUri(SAMPLE_URI)
            .withCristinOrgUri(SAMPLE_URI)
            .withActingUser(SAMPLE_ACTING_USER)
            .withScopes(scopes)
            .build();
    }
}