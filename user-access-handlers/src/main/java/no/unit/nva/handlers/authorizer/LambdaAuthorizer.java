package no.unit.nva.handlers.authorizer;

import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.commons.apigateway.authentication.AuthorizerResponse;
import no.unit.commons.apigateway.authentication.RequestAuthorizer;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LambdaAuthorizer extends RequestAuthorizer {

    public static final String DEFAULT_PRINCIPAL_ID = "ServiceAccessingIdentityService";
    public static final String AWS_SECRET_NAME_ENV_VAR = "API_SECRET_NAME";
    public static final String AWS_SECRET_KEY_ENV_VAR = "API_SECRET_KEY";
    private final AWSSecretsManager awsSecretsManager;

    private static final Logger logger = LoggerFactory.getLogger(LambdaAuthorizer.class);


    @JacocoGenerated
    public LambdaAuthorizer() {
        this(newAwsSecretsManager(), new Environment());
    }

    public LambdaAuthorizer(AWSSecretsManager awsSecretsManager, Environment environment) {
        super(environment);
        this.awsSecretsManager = awsSecretsManager;
    }

    @Override
    protected AuthorizerResponse processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        AuthorizerResponse response = super.processInput(input, requestInfo, context);

        try {
            logger.debug("Response: " + JsonUtils.dtoObjectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return response;
    }

    @Override
    protected String principalId() {
        return DEFAULT_PRINCIPAL_ID;
    }

    @Override
    protected String fetchSecret() {
        final String secretName = environment.readEnv(AWS_SECRET_NAME_ENV_VAR);
        final String secretKey = environment.readEnv(AWS_SECRET_KEY_ENV_VAR);
        SecretsReader secretsReader = new SecretsReader(awsSecretsManager);
        return attempt(() -> secretsReader.fetchSecret(secretName, secretKey))
                .orElseThrow();
    }

    @JacocoGenerated
    private static AWSSecretsManager newAwsSecretsManager() {
        return AWSSecretsManagerClientBuilder.defaultClient();
    }
}