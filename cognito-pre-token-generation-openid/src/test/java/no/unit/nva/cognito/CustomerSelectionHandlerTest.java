package no.unit.nva.cognito;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.FakeCognito;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Set;

import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerSelectionHandlerTest {

    private final FakeContext context = new FakeContext();
    private FakeCognito cognito;
    private CustomerSelectionHandler handler;
    private ByteArrayOutputStream outputStream;
    private final Set<URI> allowedCustomers = Set.of(randomUri(), randomUri());

    @BeforeEach
    void init() {
        outputStream = new ByteArrayOutputStream();
        cognito = new FakeCognito("test-client");
        var environment = mock(Environment.class);
        when(environment.readEnv("API_HOST")).thenReturn("localhost");
        when(environment.readEnv("USER_POOL_ID")).thenReturn("userpool-1");
        when(environment.readEnv("AWS_REGION")).thenReturn("eu-west-1");
        when(environment.readEnv("COGNITO_AUTHORIZER_URLS")).thenReturn("http://localhost:3000");
        when(environment.readEnv("ALLOWED_ORIGIN")).thenReturn("*");
        this.handler = new CustomerSelectionHandler(cognito, environment);
    }

    @Test
    void shouldUpdateSelectedCustomerWhenValidOptions()
        throws IOException {
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        var response = sendRequest(input, Void.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        var updatedSelectedCustomer = extractAttributeUpdate(CURRENT_CUSTOMER_CLAIM);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(URI.create(updatedSelectedCustomer), is(in(allowedCustomers.toArray(URI[]::new))));
    }

    @Test
    void shouldNotUpdateSelectedCustomerWhenInvalidOptions()
        throws IOException {
        var invalidCustomer = randomUri();
        var input = createRequest(invalidCustomer);
        var response = sendRequest(input, Void.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream input, Class<T> responseType) throws IOException {
        handler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private String extractAttributeUpdate(String attributeName) {
        var request = cognito.getAdminUpdateUserRequest();
        return request
                   .userAttributes()
                   .stream()
                   .filter(attribute -> attributeName.equals(attribute.name()))
                   .map(AttributeType::value)
                   .collect(SingletonCollector.collect());
    }

    private InputStream createRequest(URI customerId) throws JsonProcessingException {
        var randomCustomer = CustomerSelection.fromCustomerId(customerId);
        return new HandlerRequestBuilder<CustomerSelection>(dtoObjectMapper)
                   .withPersonCristinId(randomUri())
                   .withCurrentCustomer(randomUri())
                   .withAllowedCustomers(allowedCustomers)
                   .withUserName(randomString())
                   .withCognitoUsername(randomString())
                   .withBody(randomCustomer)
                   .build();
    }
}