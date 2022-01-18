package no.unit.nva.handlers.authorizer;

import no.unit.commons.apigateway.authentication.DefaultRequestAuthorizer;
import nva.commons.core.JacocoGenerated;

public class LambdaAuthorizer extends DefaultRequestAuthorizer {

    public static final String DEFAULT_PRINCIPAL_ID = "ServiceAccessingIdentityService";

    @JacocoGenerated
    public LambdaAuthorizer() {
        super(DEFAULT_PRINCIPAL_ID);
    }
}