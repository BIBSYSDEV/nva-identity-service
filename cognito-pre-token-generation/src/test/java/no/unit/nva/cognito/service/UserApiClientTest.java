package no.unit.nva.cognito.service;

import static java.util.Collections.singletonList;
import static no.unit.nva.cognito.service.UserApiClient.COULD_NOT_CREATE_USER_ERROR_MESSAGE;
import static no.unit.nva.cognito.service.UserApiClient.COULD_NOT_FETCH_USER_ERROR_MESSAGE;
import static no.unit.nva.cognito.service.UserApiClient.ERROR_PARSING_USER_INFORMATION;
import static no.unit.nva.cognito.service.UserApiClient.USER_API_HOST;
import static no.unit.nva.cognito.service.UserApiClient.USER_API_SCHEME;
import static no.unit.nva.cognito.service.UserApiClient.USER_SERVICE_SECRET_KEY;
import static no.unit.nva.cognito.service.UserApiClient.USER_SERVICE_SECRET_NAME;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import no.unit.nva.cognito.exception.BadGatewayException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.Environment;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@SuppressWarnings("unchecked")
public class UserApiClientTest {

    public static final String GARBAGE_JSON = "{{}";
    public static final String SAMPLE_USERNAME = "user@name";
    public static final String SAMPLE_INSTITUTION_ID = "institution.id";
    public static final String CREATOR = "Creator";
    public static final String SAMPLE_API_SCHEME = "http";
    public static final String SAMPLE_API_HOST = "example.org";
    public static final String SAMPLE_FAMILY_NAME = "familyName";
    public static final String SAMPLE_GIVEN_NAME = "givenName";
    public static final String SOME_SECRET = "someSecret";

    private ObjectMapper objectMapper;
    private UserApiClient userApiClient;
    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;

    /**
     * Set up test environment.
     */
    @BeforeEach
    public void init() throws ErrorReadingSecretException {
        objectMapper = new ObjectMapper();

        Environment environment = mock(Environment.class);
        when(environment.readEnv(USER_API_SCHEME)).thenReturn(SAMPLE_API_SCHEME);
        when(environment.readEnv(USER_API_HOST)).thenReturn(SAMPLE_API_HOST);
        when(environment.readEnv(USER_SERVICE_SECRET_NAME)).thenReturn(USER_SERVICE_SECRET_NAME);
        when(environment.readEnv(USER_SERVICE_SECRET_KEY)).thenReturn(USER_SERVICE_SECRET_KEY);
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);

        SecretsReader secretsReader = mockSecretsReader();
        userApiClient = new UserApiClient(httpClient, new ObjectMapper(), secretsReader, environment);
    }

    @Test
    public void getUserReturnsUserOnValidUsername() throws Exception {
        httpResponse = successfulGetResponse();
        when(httpClient.send(any(), any())).thenAnswer(invocation -> httpResponse);
        Optional<UserDto> user = userApiClient.getUser(SAMPLE_USERNAME);
        assertTrue(user.isPresent());
    }

    @Test
    public void getUserReturnsEmptyOptionalOnInvalidJsonResponse()
        throws IOException, InterruptedException, BadGatewayException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpResponse.body()).thenReturn(GARBAGE_JSON);
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenAnswer(invocation -> httpResponse);

        Executable action = () -> userApiClient.getUser(SAMPLE_USERNAME);

        assertThrows(IllegalStateException.class, action);
        String messages = appender.getMessages();
        assertThat(messages, containsString(ERROR_PARSING_USER_INFORMATION));
    }

    @Test
    public void getUserReturnsEmptyOptionalOnInvalidHttpResponse()
        throws IOException, InterruptedException, BadGatewayException {
        final TestAppender logAppendeer = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpResponse.statusCode()).thenReturn(SC_INTERNAL_SERVER_ERROR);
        when(httpClient.send(any(), any())).thenAnswer(invocation -> httpResponse);

        Executable action = () -> userApiClient.getUser(SAMPLE_USERNAME);

        assertThrows(BadGatewayException.class, action);
        assertThat(logAppendeer.getMessages(), containsString(COULD_NOT_FETCH_USER_ERROR_MESSAGE));
    }

    @Test
    public void getUserReturnsEmptyOptionalOnHttpError() throws IOException, InterruptedException, BadGatewayException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        Executable action = () -> userApiClient.getUser(SAMPLE_USERNAME);
        assertThrows(BadGatewayException.class, action);
        String messages = appender.getMessages();
        assertThat(messages, containsString(COULD_NOT_FETCH_USER_ERROR_MESSAGE));
    }

    @Test
    public void createUserReturnsCreatedUserOnSuccess()
        throws IOException, InterruptedException, InvalidEntryInternalException, BadGatewayException {
        when(httpResponse.body()).thenReturn(getValidJsonUser());
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenAnswer(invocation -> httpResponse);

        UserDto requestUser = sampleUser();

        UserDto responseUser = userApiClient.createUser(requestUser);

        Assertions.assertNotNull(responseUser);
    }

    @Test
    public void createUserReturnsErrorOnFailure()
        throws IOException, InterruptedException, InvalidEntryInternalException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        UserDto requestUser = sampleUser();

        Exception exception = assertThrows(BadGatewayException.class,
                                           () -> userApiClient.createUser(requestUser));

        assertEquals(COULD_NOT_CREATE_USER_ERROR_MESSAGE, exception.getMessage());
        String messages = appender.getMessages();
        assertThat(messages, containsString(COULD_NOT_CREATE_USER_ERROR_MESSAGE));
    }

    @Test
    public void updateUserSendsAPutRequestToUserServiceWithUserIdentifierInPathAndUserInBody()
        throws InvalidEntryInternalException, IOException, InterruptedException {

        AtomicReference<Boolean> requestIsReceived = new AtomicReference<>();
        requestIsReceived.set(false);
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
            .thenAnswer((invocation -> {
                HttpRequest request = invocation.getArgument(0);
                return assertThatPutRequestContainsBodyAndCorrectMethod(requestIsReceived, request);
            }));

        userApiClient.updateUser(sampleUser());
        assertThat(requestIsReceived.get(), is(true));
    }

    @Test
    public void updateUserThrowsBadGatewayExceptionIfResponseIsNotSuccessful()
        throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
            .thenAnswer(invocation -> mockResponse(HttpURLConnection.HTTP_UNAUTHORIZED));

        Executable action = () -> userApiClient.updateUser(sampleUser());
        assertThrows(BadGatewayException.class, action);
    }

    public String getValidJsonUser() throws JsonProcessingException, InvalidEntryInternalException {
        return objectMapper.writeValueAsString(sampleUser());
    }

    private HttpResponse<String> assertThatPutRequestContainsBodyAndCorrectMethod(
        AtomicReference<Boolean> requestIsReceived,
        HttpRequest request) {
        Long contentLength = request.bodyPublisher().map(BodyPublisher::contentLength).orElse(0L);
        assertThat(request.method(), is(equalTo("PUT")));
        assertThat(contentLength, is(greaterThan(0L)));
        requestIsReceived.set(true);
        return mockResponse(HttpURLConnection.HTTP_ACCEPTED);
    }

    private SecretsReader mockSecretsReader() throws ErrorReadingSecretException {
        SecretsReader secretsReader = mock(SecretsReader.class);
        when(secretsReader.fetchSecret(anyString(), anyString())).thenReturn(SOME_SECRET);
        return secretsReader;
    }

    private HttpResponse<String> successfulGetResponse() throws InvalidEntryInternalException, JsonProcessingException {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(getValidJsonUser());
        when(response.statusCode()).thenReturn(SC_OK);
        return response;
    }

    private HttpResponse<String> mockResponse(int statusCode) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        return response;
    }

    private UserDto sampleUser() throws InvalidEntryInternalException {
        return UserDto.newBuilder()
                   .withRoles(singletonList(RoleDto.newBuilder().withName(CREATOR).build()))
                   .withUsername(SAMPLE_USERNAME)
                   .withInstitution(SAMPLE_INSTITUTION_ID)
                   .withGivenName(SAMPLE_GIVEN_NAME)
                   .withFamilyName(SAMPLE_FAMILY_NAME)
                   .build();
    }
}
