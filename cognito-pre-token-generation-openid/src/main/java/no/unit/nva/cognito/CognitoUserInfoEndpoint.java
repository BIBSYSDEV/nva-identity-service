package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Set;
import no.unit.nva.auth.CognitoUserInfo;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;

public class CognitoUserInfoEndpoint extends ApiGatewayHandler<Void, CognitoUserInfo> {

    public static final String SAMPLE = "anything";
    public static final URI SAMPLE_URI = URI.create("https://example.org");

    public CognitoUserInfoEndpoint() {
        super(Void.class);
    }

    @Override
    protected CognitoUserInfo processInput(Void input, RequestInfo requestInfo, Context context) {
        return CognitoUserInfo.builder()
            .withCurrentCustomer(SAMPLE_URI)
            .withNvaUsername(SAMPLE)
            .withTopOrgCristinId(SAMPLE_URI)
            .withCurrentCustomer(SAMPLE_URI)
            .withSub(SAMPLE)
            .withRoles(SAMPLE)
            .withAccessRights(Set.of(AccessRight.APPROVE_DOI_REQUEST.toString()))
            .withCognitoUsername(SAMPLE)
            .withPersonAffiliation(SAMPLE)
            .withFeideId(SAMPLE)
            .withPersonCristinId(SAMPLE_URI)
            .withAllowedCustomers(SAMPLE)
            .build();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CognitoUserInfo output) {
        return HttpURLConnection.HTTP_OK;
    }
}
