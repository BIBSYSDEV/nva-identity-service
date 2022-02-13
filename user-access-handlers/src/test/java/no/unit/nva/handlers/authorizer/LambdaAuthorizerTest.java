package no.unit.nva.handlers.authorizer;

import static no.unit.commons.apigateway.authentication.DefaultRequestAuthorizer.API_KEY_SECRET_KEY;
import static no.unit.commons.apigateway.authentication.DefaultRequestAuthorizer.API_KEY_SECRET_NAME;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import no.unit.commons.apigateway.authentication.AuthorizerResponse;
import no.unit.commons.apigateway.authentication.StatementElement;
import no.unit.nva.database.interfaces.WithEnvironment;
import no.unit.nva.testutils.HandlerRequestBuilder;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class LambdaAuthorizerTest implements WithEnvironment {

    public static final String CORRECT_SECRET_VALUE = "someSecretValue";
    public static final String AWS_SERCRETS_MANAGER_ERROR_MESSAGE = "AwsSercretsManager message";
    public static final int SINGLE_STATEMENT_EXPECTED = 1;
    public static final int UNIQUE_STATEMENT = 0;
    public static final String ACTION_ALLOWED = "Allow";
    public static final String ACTION_DENIED = "Deny";

    public static final String DEFAULT_METHOD_ARN = "arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod/GET"
                                                    + "/service/users/orestis@unit.no";
    public static final String METHOD_ARN_REQUEST_FIELD = "methodArn";
    private SecretsManagerClient secretsManager;

    private final Context context;

    public LambdaAuthorizerTest() {
        context = mock(Context.class);
    }

    @BeforeEach
    public void init() {
        this.secretsManager = secretsManager();
    }

    @Test
    void shouldReturnAcceptPolicyWhenSecretIsCorrect() throws IOException {

        AuthorizerResponse response = sendRequest(CORRECT_SECRET_VALUE);
        List<StatementElement> statements = response.getPolicyDocument().getStatement();
        assertThat(statements.size(), is(equalTo(SINGLE_STATEMENT_EXPECTED)));

        StatementElement statement = statements.get(UNIQUE_STATEMENT);
        String actualEffect = statement.getEffect();
        assertThat(actualEffect, is(equalTo(ACTION_ALLOWED)));
    }

    @Test
    void shouldReturnDenyPolicyWhenSecretNameIsWrong() throws IOException {
        secretsManagerCannotFindTheSecret();
        AuthorizerResponse response = sendRequest(randomString());

        List<StatementElement> statements = response.getPolicyDocument().getStatement();
        assertThat(statements.size(), is(equalTo(SINGLE_STATEMENT_EXPECTED)));

        StatementElement statement = statements.get(UNIQUE_STATEMENT);
        String actualEffect = statement.getEffect();
        assertThat(actualEffect, is(equalTo(ACTION_DENIED)));
    }

    @Test
    void shouldReturnDenyPolicyWhenSecretKeyIsWrong() throws IOException {
        secretsManagerReturnsSecretForRequestedSecretNameButSecretDoesNotContainExpetedKey();
        AuthorizerResponse response = sendRequest(randomString());

        List<StatementElement> statements = response.getPolicyDocument().getStatement();
        assertThat(statements.size(), is(equalTo(SINGLE_STATEMENT_EXPECTED)));

        StatementElement statement = statements.get(UNIQUE_STATEMENT);
        String actualEffect = statement.getEffect();
        assertThat(actualEffect, is(equalTo(ACTION_DENIED)));
    }

    private void secretsManagerCannotFindTheSecret() {
        secretsManager = mock(SecretsManagerClient.class);
        when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
            .thenAnswer(ignored -> {
                throw ResourceNotFoundException.builder().build();
            });
    }

    private void secretsManagerReturnsSecretForRequestedSecretNameButSecretDoesNotContainExpetedKey() {
        secretsManager = mock(SecretsManagerClient.class);
        when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
            .thenAnswer(ignored -> secretWithCorrectNameButWrongKey());
    }

    private GetSecretValueResponse secretWithCorrectNameButWrongKey() throws JsonProcessingException {
        return GetSecretValueResponse.builder()
            .secretString(createSecretAsJson(randomString(), randomString()))
            .build();
    }

    private SecretsManagerClient secretsManager() {
        SecretsManagerClient awsSecretsManager = mock(SecretsManagerClient.class);
        when(awsSecretsManager.getSecretValue(any(GetSecretValueRequest.class)))
            .thenAnswer(provideSecret());
        return awsSecretsManager;
    }

    private AuthorizerResponse sendRequest(String submittedSecret) throws IOException {
        LambdaAuthorizer authorizer = new LambdaAuthorizer(secretsManager);
        InputStream request = buildRequest(submittedSecret);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        authorizer.handleRequest(request, outputStream, context);
        return AuthorizerResponse.fromOutputStream(outputStream);
    }

    private InputStream buildRequest(String submittedSecret) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
            .withHeaders(Map.of(HttpHeaders.AUTHORIZATION, submittedSecret))
            .withOtherProperties(Map.of(METHOD_ARN_REQUEST_FIELD, DEFAULT_METHOD_ARN))
            .build();
    }

    private Answer<GetSecretValueResponse> provideSecret() {
        return invocation -> {
            GetSecretValueRequest request = invocation.getArgument(0);
            if (request.secretId().equals(API_KEY_SECRET_NAME)) {
                String secret = createSecretAsJson(API_KEY_SECRET_KEY, CORRECT_SECRET_VALUE);
                return GetSecretValueResponse.builder()
                    .name(API_KEY_SECRET_NAME)
                    .secretString(secret)
                    .build();
            }
            throw new RuntimeException(AWS_SERCRETS_MANAGER_ERROR_MESSAGE);
        };
    }

    private String createSecretAsJson(String secretKey, String secreteValue) throws JsonProcessingException {
        Map<String, String> secret = Map.of(secretKey, secreteValue);
        return dtoObjectMapper.writeValueAsString(secret);
    }
}