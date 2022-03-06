package no.unit.nva.handlers.authorizer;

import static no.unit.commons.apigateway.authentication.DefaultRequestAuthorizer.API_KEY_SECRET_KEY;
import static no.unit.commons.apigateway.authentication.DefaultRequestAuthorizer.API_KEY_SECRET_NAME;
import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import java.util.List;
import java.util.Map;
import no.unit.commons.apigateway.authentication.AuthorizerResponse;
import no.unit.commons.apigateway.authentication.StatementElement;
import no.unit.nva.database.interfaces.WithEnvironment;
import no.unit.nva.stubs.FakeContext;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

class LambdaAuthorizerTest implements WithEnvironment {

    public static final String CORRECT_SECRET_VALUE = "someSecretValue";
    public static final String AWS_SERCRETS_MANAGER_ERROR_MESSAGE = "AwsSercretsManager message";
    public static final int SINGLE_STATEMENT_EXPECTED = 1;
    public static final int UNIQUE_STATEMENT = 0;
    public static final String ACTION_ALLOWED = "Allow";
    public static final String ACTION_DENIED = "Deny";

    public static final String DEFAULT_METHOD_ARN = "arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod/GET"
                                                    + "/service/users/orestis@unit.no";
    private final Context context;
    private SecretsManagerClient secretsManager;

    public LambdaAuthorizerTest() {
        context = new FakeContext();
    }

    @BeforeEach
    public void init() {
        this.secretsManager = secretsManager();
    }

    @Test
    void shouldReturnAcceptPolicyWhenSecretIsCorrect() {

        AuthorizerResponse response = sendRequest(CORRECT_SECRET_VALUE);
        List<StatementElement> statements = response.getPolicyDocument().getStatement();
        assertThat(statements.size(), is(equalTo(SINGLE_STATEMENT_EXPECTED)));

        StatementElement statement = statements.get(UNIQUE_STATEMENT);
        String actualEffect = statement.getEffect();
        assertThat(actualEffect, is(equalTo(ACTION_ALLOWED)));
    }

    @Test
    void shouldReturnDenyPolicyWhenSecretNameIsWrong() {
        secretsManagerCannotFindTheSecret();
        AuthorizerResponse response = sendRequest(randomString());

        List<StatementElement> statements = response.getPolicyDocument().getStatement();
        assertThat(statements.size(), is(equalTo(SINGLE_STATEMENT_EXPECTED)));

        StatementElement statement = statements.get(UNIQUE_STATEMENT);
        String actualEffect = statement.getEffect();
        assertThat(actualEffect, is(equalTo(ACTION_DENIED)));
    }

    @Test
    void shouldReturnDenyPolicyWhenSecretKeyIsWrong() {
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

    private GetSecretValueResponse secretWithCorrectNameButWrongKey() {
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

    private AuthorizerResponse sendRequest(String submittedSecret) {
        var authorizer = new LambdaAuthorizer(secretsManager);
        var request = buildRequest(submittedSecret);
        return authorizer.handleRequest(request, context);
    }

    private APIGatewayCustomAuthorizerEvent buildRequest(String submittedSecret) {
        return APIGatewayCustomAuthorizerEvent.builder()
            .withHeaders(Map.of(HttpHeaders.AUTHORIZATION, submittedSecret))
            .withMethodArn(DEFAULT_METHOD_ARN)
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

    private String createSecretAsJson(String secretKey, String secreteValue) {
        Map<String, String> secret = Map.of(secretKey, secreteValue);
        return attempt(() -> objectMapper.asString(secret)).orElseThrow();
    }
}