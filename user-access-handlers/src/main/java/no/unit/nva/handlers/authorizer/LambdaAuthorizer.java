package no.unit.nva.handlers.authorizer;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.commons.apigateway.authentication.RequestAuthorizer;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class LambdaAuthorizer extends RequestAuthorizer {

    public static final String DEFAULT_PRINCIPAL_ID = "ServiceAccessingIdentityService";
    public static final String AWS_SECRET_NAME_ENV_VAR = "API_SECRET_NAME";
    public static final String AWS_SECRET_KEY_ENV_VAR = "API_SECRET_KEY";
    private final SecretsManagerClient awsSecretsManager;

    @JacocoGenerated
    public LambdaAuthorizer() {
        this(SecretsReader.defaultSecretsManagerClient(), new Environment());
    }

    public LambdaAuthorizer(SecretsManagerClient awsSecretsManager, Environment environment) {
        super(environment);
        this.awsSecretsManager = awsSecretsManager;
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
}