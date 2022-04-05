package no.unit.nva.handlers.authorizer;

import no.unit.commons.apigateway.authentication.DefaultRequestAuthorizer;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class LambdaAuthorizer extends DefaultRequestAuthorizer {

    public static final String DEFAULT_PRINCIPAL_ID = "ServiceAccessingIdentityService";

    @JacocoGenerated
    public LambdaAuthorizer() {
        super(DEFAULT_PRINCIPAL_ID);
    }

    public LambdaAuthorizer(SecretsManagerClient secretsManagerClient) {
        super(secretsManagerClient, DEFAULT_PRINCIPAL_ID);
    }
}