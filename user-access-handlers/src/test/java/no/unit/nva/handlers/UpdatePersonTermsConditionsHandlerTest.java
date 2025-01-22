package no.unit.nva.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import no.unit.nva.cognito.CognitoClaims;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.database.TermsAndConditionsService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdatePersonTermsConditionsHandlerTest extends HandlerTest {

    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final String USERNAME_CLAIM = "username";
    private FakeContext context;
    private ByteArrayOutputStream outputStream;
    private UpdatePersonTermsConditionsHandler handler;
    private TermsAndConditionsService mockedService;
    private TermsConditionsResponse response;
    private CognitoIdentityProviderClient cognito;

    @BeforeEach
    void setup() {
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
        mockedService = mock(TermsAndConditionsService.class);
        cognito = mock(CognitoIdentityProviderClient.class);
        var environment = mock(Environment.class);
        when(environment.readEnv("USER_POOL_ID")).thenReturn("USER_POOL_ID");
        when(environment.readEnv("COGNITO_HOST")).thenReturn("COGNITO_HOST");
        when(environment.readEnv("API_HOST")).thenReturn("API_HOST");
        when(environment.readEnv("AWS_REGION")).thenReturn("AWS_REGION");
        handler = new UpdatePersonTermsConditionsHandler(mockedService, cognito, environment);
        response = TermsConditionsResponse.builder()
            .withTermsConditionsUri(randomUri())
            .build();
    }

    @Test
    void shouldReturnTheClientWhenItExists() throws IOException, NotFoundException {
        when(mockedService.updateTermsAndConditions(any(), any(), any())).thenReturn(response);
        when(mockedService.getCurrentTermsAndConditions()).thenReturn(response);

        var gatewayResponse = sendRequest(getInputStream(), TermsConditionsResponse.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(gatewayResponse.getBodyObject(TermsConditionsResponse.class), is(equalTo(response)));
    }

    @Test
    void shouldUpdateCognitoWithLatestTermsSelection() throws IOException, NotFoundException {
        when(mockedService.updateTermsAndConditions(any(), any(), any())).thenReturn(response);
        when(mockedService.getCurrentTermsAndConditions()).thenReturn(response);

        sendRequest(getInputStream(), TermsConditionsResponse.class);

        ArgumentCaptor<AdminUpdateUserAttributesRequest> captor = forClass(AdminUpdateUserAttributesRequest.class);
        verify(cognito, atLeastOnce()).adminUpdateUserAttributes(captor.capture());

        AdminUpdateUserAttributesRequest capturedRequest = captor.getValue();
        List<AttributeType> capturedAttributes = capturedRequest.userAttributes();

        assertTrue(capturedAttributes.stream().anyMatch(attribute ->
                                                            attribute.name().equals(CognitoClaims.CUSTOMER_ACCEPTED_TERMS))
        );

        assertTrue(capturedAttributes.stream().anyMatch(attribute ->
                                                            attribute.name().equals(CognitoClaims.CURRENT_TERMS)
        ));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream getInputStream() throws JsonProcessingException {
        return new HandlerRequestBuilder<TermsConditionsResponse>(objectMapperWithEmpty)
                   .withRequestContext(getRequestContext())
                   .withPersonCristinId(randomUri())
                   .withCurrentCustomer(randomUri())
                   .withHeaders(getRequestHeaders())
                   .withUserName(randomString())
                   .withAuthorizerClaim(USERNAME_CLAIM, randomString())
                   .withBody(response)
                   .build();
    }

    private Map<String, String> getRequestHeaders() {
        return Map.of("Content-Type", "application/json", "Authorization", "Bearer " + randomString());
    }

    private ObjectNode getRequestContext() {
        return objectMapperWithEmpty.convertValue(
            Map.of("path", "/terms-and-conditions/", "domainName", "SAMPLE_DOMAIN_NAME"), ObjectNode.class);
    }
}