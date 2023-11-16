package no.unit.nva.handlers;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.handlers.SetImpersonationHandler.IMPERSONATION;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.ADMINISTRATE_APPLICATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.FakeCognito;
import no.unit.nva.handlers.models.ImpersonationRequest;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

class SetImpersonationHandlerTest {

    public static final String USERNAME = "username";
    private FakeCognito cognitoClient;
    private SetImpersonationHandler handler;
    private final Context context = new FakeContext();
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void setup() {
        cognitoClient = new FakeCognito(randomString());
        handler = new SetImpersonationHandler(cognitoClient);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void shouldReturn200okWhenUserIsAppAdminAndIsNotImpersonatingSomeoneAlready() throws IOException {
        var request = createDefaultRequestForImpersonation(randomString());
        handler.handleRequest(request, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
    }


    @Test
    public void shouldCallCognitoAdminApiWithImpersonatedFnrWhenUserIsAppAdminAndIsNotImpersonatingSomeoneAlready()
        throws IOException {
        var impersonationFnr = randomString();
        var request = createDefaultRequestForImpersonation(impersonationFnr);
        handler.handleRequest(request, outputStream, context);
        var setImpersonationClaim = extractAdminUpdateRequestUserAttribute(IMPERSONATION);
        assertThat(setImpersonationClaim, is(equalTo(impersonationFnr)));
    }

    @Test
    public void shouldReturnForbiddenIfUserIsNotAppAdmin() throws IOException {
        var inputStream = createNonAdministratorRequestForImpersonation(randomString());
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    public void shouldReturnForbiddenIfUserAlreadyImpersonating() throws IOException {
        var inputStream = createAlreadyImpersonatingRequestForImpersonation(randomString());
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    private InputStream createDefaultRequestForImpersonation(String impersonatingFnr) throws JsonProcessingException {
        var customer = randomUri();
        return new HandlerRequestBuilder<ImpersonationRequest>(dtoObjectMapper)
                   .withBody(new ImpersonationRequest(impersonatingFnr))
                   .withCurrentCustomer(customer)
                   .withAccessRights(customer, ADMINISTRATE_APPLICATION.toString())
                   .withAuthorizerClaim(USERNAME, randomString())
                   .withIssuer(randomString())
                   .withUserName(randomString())
                   .build();
    }

    private InputStream createNonAdministratorRequestForImpersonation(String impersonatingFnr)
            throws JsonProcessingException {
        var customer = randomUri();
        return new HandlerRequestBuilder<ImpersonationRequest>(dtoObjectMapper)
                   .withBody(new ImpersonationRequest(impersonatingFnr))
                   .withCurrentCustomer(customer)
                   .withAuthorizerClaim(USERNAME, randomString())
                   .withIssuer(randomString())
                   .withUserName(randomString())
                   .build();
    }

    private InputStream createAlreadyImpersonatingRequestForImpersonation(String impersonatingFnr)
        throws JsonProcessingException {
        var customer = randomUri();
        return new HandlerRequestBuilder<ImpersonationRequest>(dtoObjectMapper)
                   .withBody(new ImpersonationRequest(impersonatingFnr))
                   .withCurrentCustomer(customer)
                   .withAccessRights(customer, ADMINISTRATE_APPLICATION.toString())
                   .withAuthorizerClaim(IMPERSONATION, randomString())
                   .withAuthorizerClaim(USERNAME, randomString())
                   .withIssuer(randomString())
                   .withUserName(randomString())
                   .build();
    }

    private String extractAdminUpdateRequestUserAttribute(String userAttribute) {
        return cognitoClient.getAdminUpdateUserRequest()
                   .userAttributes().stream()
                   .filter(attribute -> attribute.name().equals(userAttribute))
                   .map(AttributeType::value)
                   .findFirst()
                   .orElseThrow();
    }

}