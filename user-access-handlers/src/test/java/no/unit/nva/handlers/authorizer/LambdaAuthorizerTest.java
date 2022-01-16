package no.unit.nva.handlers.authorizer;

import static no.unit.nva.useraccessmanagement.RestConfig.defaultRestObjectMapper;
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
import nva.commons.core.Environment;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class LambdaAuthorizerTest implements WithEnvironment {

    public static final String CORRECT_SECRET_NAME = "someSecretName";
    public static final String CORRECT_SECRET_KEY = "someSecretKey";
    public static final String CORRECT_SECRET_VALUE = "someSecretValue";
    public static final String AWS_SERCRETS_MANAGER_ERROR_MESSAGE = "AwsSercretsManager message";
    public static final String DEFAULT_ENV_VALUE = "*";
    public static final int SINGLE_STATEMENT_EXPECTED = 1;
    public static final int UNIQUE_STATEMENT = 0;
    public static final String ACTION_ALLOWED = "Allow";
    public static final String ACTION_DENIED = "Deny";

    public static final String DEFAULT_METHOD_ARN = "arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod/GET"
                                                    + "/service/users/orestis@unit.no";
    public static final String METHOD_ARN_REQUEST_FIELD = "methodArn";
    public static final String WRONG_SECRET_NAME = "WrongSecretName";
    private static final String WRONG_SECRET_KEY = "WrongSecretKey";

    private final Environment envWithCorrectValues;
    private Context context;

    public LambdaAuthorizerTest() {
        secretsManager();
        Map<String, String> correctEnvValues = Map.of(LambdaAuthorizer.AWS_SECRET_NAME_ENV_VAR, CORRECT_SECRET_NAME,
                                                      LambdaAuthorizer.AWS_SECRET_KEY_ENV_VAR, CORRECT_SECRET_KEY);
        envWithCorrectValues = mockEnvironment(correctEnvValues, DEFAULT_ENV_VALUE);
        context = mock(Context.class);
    }

    @Test
    void shouldReturnAcceptPolicyWhenSecretIsCorrect() throws IOException {

        AuthorizerResponse response = sendRequest(envWithCorrectValues);

        List<StatementElement> statements = response.getPolicyDocument().getStatement();
        assertThat(statements.size(), is(equalTo(SINGLE_STATEMENT_EXPECTED)));

        StatementElement statement = statements.get(UNIQUE_STATEMENT);
        String actualEffect = statement.getEffect();
        assertThat(actualEffect, is(equalTo(ACTION_ALLOWED)));
    }

    @Test
    void shouldReturnDenyPolicyWhenSecretNameIsWrong() throws IOException {
        Map<String, String> wrongSecretName = Map.of(
            LambdaAuthorizer.AWS_SECRET_NAME_ENV_VAR, WRONG_SECRET_NAME,
            LambdaAuthorizer.AWS_SECRET_KEY_ENV_VAR, CORRECT_SECRET_KEY);

        Environment envWithWrongSecretName = mockEnvironment(wrongSecretName, DEFAULT_ENV_VALUE);
        AuthorizerResponse response = sendRequest(envWithWrongSecretName);

        List<StatementElement> statements = response.getPolicyDocument().getStatement();
        assertThat(statements.size(), is(equalTo(SINGLE_STATEMENT_EXPECTED)));

        StatementElement statement = statements.get(UNIQUE_STATEMENT);
        String actualEffect = statement.getEffect();
        assertThat(actualEffect, is(equalTo(ACTION_DENIED)));
    }

    @Test
    void shouldReturnDenyPolicyWhenSecretKeyIsWrong() throws IOException {
        Map<String, String> wrongSecretName = Map.of(
            LambdaAuthorizer.AWS_SECRET_NAME_ENV_VAR, CORRECT_SECRET_NAME,
            LambdaAuthorizer.AWS_SECRET_KEY_ENV_VAR, WRONG_SECRET_KEY);

        Environment envWithWrongSecretName = mockEnvironment(wrongSecretName, DEFAULT_ENV_VALUE);
        AuthorizerResponse response = sendRequest(envWithWrongSecretName);

        List<StatementElement> statements = response.getPolicyDocument().getStatement();
        assertThat(statements.size(), is(equalTo(SINGLE_STATEMENT_EXPECTED)));

        StatementElement statement = statements.get(UNIQUE_STATEMENT);
        String actualEffect = statement.getEffect();
        assertThat(actualEffect, is(equalTo(ACTION_DENIED)));
    }

    private SecretsManagerClient secretsManager() {
        SecretsManagerClient awsSecretsManager = mock(SecretsManagerClient.class);
        when(awsSecretsManager.getSecretValue(any(GetSecretValueRequest.class)))
            .thenAnswer(provideSecret());
        return awsSecretsManager;
    }

    private AuthorizerResponse sendRequest(Environment environment) throws IOException {
        LambdaAuthorizer authorizer = new LambdaAuthorizer(secretsManager(), environment);
        InputStream request = buildRequest();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        authorizer.handleRequest(request, outputStream, context);
        return AuthorizerResponse.fromOutputStream(outputStream);
    }

    private InputStream buildRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(defaultRestObjectMapper)
            .withHeaders(Map.of(HttpHeaders.AUTHORIZATION, CORRECT_SECRET_VALUE))
            .withOtherProperties(Map.of(METHOD_ARN_REQUEST_FIELD, DEFAULT_METHOD_ARN))
            .build();
    }

    private Answer<GetSecretValueResponse> provideSecret() {
        return invocation -> {
            GetSecretValueRequest request = invocation.getArgument(0);
            if (request.secretId().equals(CORRECT_SECRET_NAME)) {
                String secret = createSecretAsJson(CORRECT_SECRET_KEY, CORRECT_SECRET_VALUE);
                return GetSecretValueResponse.builder()
                    .name(CORRECT_SECRET_NAME)
                    .secretString(secret)
                    .build();
            }
            throw new RuntimeException(AWS_SERCRETS_MANAGER_ERROR_MESSAGE);
        };
    }

    private String createSecretAsJson(String secretKey, String secreteValue) throws JsonProcessingException {
        Map<String, String> secret = Map.of(secretKey, secreteValue);
        return defaultRestObjectMapper.writeValueAsString(secret);
    }
}