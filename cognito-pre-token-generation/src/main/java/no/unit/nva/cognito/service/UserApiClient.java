package no.unit.nva.cognito.service;

import static java.lang.System.currentTimeMillis;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.Callable;
import no.unit.nva.cognito.exception.BadGatewayException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserApiClient implements UserApi {

    public static final String PATH = "/users-roles-internal/service/users";
    public static final String USER_API_SCHEME = "USER_API_SCHEME";
    public static final String USER_API_HOST = "USER_API_HOST";
    public static final String ERROR_PARSING_USER_INFORMATION = "Error parsing user information";
    public static final String USER_SERVICE_SECRET_NAME = "USER_SERVICE_SECRET_NAME";
    public static final String USER_SERVICE_SECRET_KEY = "USER_SERVICE_SECRET_KEY";
    public static final String AUTHORIZATION = "Authorization";
    public static final String DELIMITER = "/";
    public static final String USER_CANNOT_BE_PARSED_ERROR = "User cannot be parsed";
    public static final String COULD_NOT_FETCH_USER_ERROR_MESSAGE = "Could not fetch user: ";
    public static final String COULD_NOT_CREATE_USER_ERROR_MESSAGE = "Could not crate user: ";
    public static final String ERROR_MESSAGE_TEMPLATE = "%s\nStatus Code:%d\n:Response message:%s";
    public static final String UPDATE_USER_FAILURE_RESPONSE_LOGGING =
        "Could not update user. User service response:{},{}";
    public static final String UPDATE_USER_FAILURE = "Could not update user";
    private static final Logger logger = LoggerFactory.getLogger(UserApiClient.class);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final SecretsReader secretsReader;
    private final String userServiceSecretName;
    private final String userServiceSecretKey;
    private final String userApiScheme;
    private final String userApiHost;

    public UserApiClient(HttpClient httpClient,
                         ObjectMapper objectMapper,
                         SecretsReader secretsReader,
                         Environment environment) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.secretsReader = secretsReader;
        this.userApiScheme = environment.readEnv(USER_API_SCHEME);
        this.userApiHost = environment.readEnv(USER_API_HOST);
        this.userServiceSecretName = environment.readEnv(USER_SERVICE_SECRET_NAME);
        this.userServiceSecretKey = environment.readEnv(USER_SERVICE_SECRET_KEY);
    }

    @Override
    public Optional<UserDto> getUser(String username) {
        long start = currentTimeMillis();
        logger.info("Requesting user information for username: " + username);
        HttpResponse<String> response = fetchUserInformation(username);
        if (responseIsSuccessful(response)) {
            logger.info("getUser success took {} ms", currentTimeMillis() - start);
            return Optional.of(tryParsingUser(response));
        } else if (responseIsNotFound(response)) {
            logger.info("getUser success took {} ms", currentTimeMillis() - start);
            return Optional.empty();
        } else {
            logger.info("getUser failure took {} ms", currentTimeMillis() - start);
            throw unexpectedException(response, COULD_NOT_FETCH_USER_ERROR_MESSAGE);
        }
    }

    @Override
    @JacocoGenerated
    public UserDto createUser(UserDto user) {
        long start = currentTimeMillis();
        logger.info("Requesting user creation for username: " + user.getUsername());

        HttpResponse<String> createResponse = createNewUser(user);
        if (responseIsSuccessful(createResponse)) {
            logger.info("createUser success took {} ms", currentTimeMillis() - start);
            return tryParsingUser(createResponse);
        } else {
            logger.info("createUser failure took {} ms", currentTimeMillis() - start);
            throw unexpectedException(createResponse, COULD_NOT_CREATE_USER_ERROR_MESSAGE);
        }
    }

    @Override
    public void updateUser(UserDto user) throws IOException, InterruptedException {
        HttpRequest request = updateUserRequest(user);
        HttpResponse<String> response = sendHttpRequest(request);

        if (HttpURLConnection.HTTP_ACCEPTED != response.statusCode()) {
            logFailedResponseError(response);
            throw new BadGatewayException(UPDATE_USER_FAILURE);
        }
    }

    private HttpRequest updateUserRequest(UserDto user) {
        return attempt(() -> formUri(user.getUsername()))
                   .map(HttpRequest::newBuilder)
                   .map(builder -> builder.PUT(userAsRequestBody(user)))
                   .map(this::authorizationHeader)
                   .map(Builder::build)
                   .orElseThrow();
    }

    private void logFailedResponseError(HttpResponse<String> response) {
        logger.error(UPDATE_USER_FAILURE_RESPONSE_LOGGING, response.statusCode(), response.body());
    }

    private BadGatewayException handleFailure(Failure<HttpResponse<String>> failure, String errorMessage) {
        logger.error(errorMessage, failure.getException());
        return new BadGatewayException(errorMessage, failure.getException());
    }

    private Builder authorizationHeader(Builder builder) {
        String userServiceCredentials = fetchUserServiceCredentials();
        return builder.header(AUTHORIZATION, userServiceCredentials);
    }

    private String fetchUserServiceCredentials() {
        logger.info("userServiceSecretName:" + userServiceSecretName);
        logger.info("userServiceSecretKey:" + userServiceSecretKey);
        return attempt(() -> secretsReader.fetchSecret(userServiceSecretName, userServiceSecretKey)).orElseThrow();
    }

    private BodyPublisher userAsRequestBody(UserDto user) {
        return attempt(() -> objectMapper.writeValueAsString(user))
                   .map(BodyPublishers::ofString)
                   .orElseThrow();
    }

    private RuntimeException unexpectedException(HttpResponse<String> response, String errorPrefix) {
        String errorMessage = formatErrorMessageForFailedResponse(response, errorPrefix);
        logger.error(errorMessage);
        return new BadGatewayException(errorMessage);
    }

    private String formatErrorMessageForFailedResponse(HttpResponse<String> response, String errorPrefix) {
        return String.format(ERROR_MESSAGE_TEMPLATE, errorPrefix, response.statusCode(), response.body());
    }

    private boolean responseIsNotFound(HttpResponse<String> response) {
        return response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND;
    }

    private HttpResponse<String> createNewUser(UserDto user) {
        return
            attempt((Callable<URI>) this::formUri)
                .map(uri -> buildCreateUserRequest(uri, user))
                .map(this::sendHttpRequest)
                .orElseThrow(fail -> handleFailure(fail, COULD_NOT_CREATE_USER_ERROR_MESSAGE));
    }

    private HttpResponse<String> fetchUserInformation(String username) {
        return attempt(() -> formUri(username))
                   .map(this::buildGetUserRequest)
                   .map(this::sendHttpRequest)
                   .orElseThrow(fail -> handleFailure(fail, COULD_NOT_FETCH_USER_ERROR_MESSAGE));
    }

    private UserDto tryParsingUser(HttpResponse<String> response) {
        return attempt(() -> parseUser(response))
                   .orElseThrow(this::logErrorParsingUserInformation);
    }

    private boolean responseIsSuccessful(HttpResponse<String> response) {
        return response.statusCode() == HttpStatus.SC_OK;
    }

    private IllegalStateException logErrorParsingUserInformation(Failure<UserDto> failure) {
        logger.error(ERROR_PARSING_USER_INFORMATION, failure.getException());
        return new IllegalStateException(USER_CANNOT_BE_PARSED_ERROR);
    }

    private UserDto parseUser(HttpResponse<String> response)
        throws JsonProcessingException {
        return objectMapper.readValue(response.body(), UserDto.class);
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest httpRequest) throws IOException, InterruptedException {
        return httpClient.send(httpRequest, BodyHandlers.ofString());
    }

    private URI formUri(String username) throws URISyntaxException {
        URI uri = new URIBuilder()
                      .setScheme(userApiScheme)
                      .setHost(userApiHost)
                      .setPath(String.join(DELIMITER, PATH, username))
                      .build();
        logger.info("GET UserUri:" + uri.toString());
        return uri;
    }

    private URI formUri() throws URISyntaxException {
        URI uri = new URIBuilder()
                      .setScheme(userApiScheme)
                      .setHost(userApiHost)
                      .setPath(PATH)
                      .build();
        logger.info("POST UserUri:" + uri.toString());
        return uri;
    }

    private HttpRequest buildGetUserRequest(URI uri) {
        return Optional.of(HttpRequest.newBuilder())
                   .map(builder -> builder.uri(uri))
                   .map(this::authorizationHeader)
                   .map(Builder::GET)
                   .map(Builder::build)
                   .orElseThrow();
    }

    private HttpRequest buildCreateUserRequest(URI uri, UserDto user) {
        return Optional.of(HttpRequest.newBuilder(uri))
                   .map(this::authorizationHeader)
                   .map(builder -> builder.POST(userAsRequestBody(user)))
                   .map(Builder::build)
                   .orElseThrow();
    }
}
