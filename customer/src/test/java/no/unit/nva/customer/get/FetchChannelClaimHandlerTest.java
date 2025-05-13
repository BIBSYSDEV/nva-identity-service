package no.unit.nva.customer.get;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDto;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelClaimDto;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.get.response.ChannelClaimResponse;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class FetchChannelClaimHandlerTest extends LocalCustomerServiceDatabase {

    public static final Context CONTEXT = new FakeContext();
    private static final String IDENTIFIER = "identifier";
    private FetchChannelClaimHandler handler;
    private CustomerService customerService;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void init() {
        super.setupDatabase();
        customerService = new DynamoDBCustomerService(dynamoClient);
        handler = new FetchChannelClaimHandler(customerService, new Environment());
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnNotFoundWhenChannelClaimDoesNotExist() throws IOException {
        var request = createAuthenticatedRequest(UUID.randomUUID().toString());

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenProvidedChannelClaimIdentifierIsNotUUID() throws IOException {
        var request = createAuthenticatedRequest(randomString());

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurred() throws IOException {
        var request = createAuthenticatedRequest(UUID.randomUUID().toString());
        var handler = new FetchChannelClaimHandler(failingCustomerService(), new Environment());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_INTERNAL_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturnOkWhenChannelClaimExists() throws IOException, ApiGatewayException {
        var channelClaim = randomChannelClaimDto();
        persistCustomerWithChannelClaim(channelClaim);

        var request = createAuthenticatedRequest(channelClaim.identifier().toString());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, ChannelClaimResponse.class);
        var fetchedChannelClaim = response.getBodyObject(ChannelClaimResponse.class);

        assertEquals(constructExpectedId(channelClaim), fetchedChannelClaim.getId());
        assertEquals(HTTP_OK, response.getStatusCode());
    }

    private static URI constructExpectedId(ChannelClaimDto channelClaim) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild("customer")
                   .addChild("channel-claim")
                   .addChild(channelClaim.identifier().toString())
                   .getUri();
    }

    private static CustomerService failingCustomerService() {
        var failingCustomerService = mock(CustomerService.class);
        when(failingCustomerService.getChannelClaim(any())).thenThrow(RuntimeException.class);
        return failingCustomerService;
    }

    private InputStream createAuthenticatedRequest(String identifier) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .withPathParameters(Map.of(IDENTIFIER, identifier))
                   .build();
    }

    private InputStream createRequest(String identifier) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withPathParameters(Map.of(IDENTIFIER, identifier))
                   .build();
    }

    private void persistCustomerWithChannelClaim(ChannelClaimDto channelClaim) throws ApiGatewayException {
        var customer = createSampleCustomerDto().copy().withChannelClaims(List.of(channelClaim)).build();
        customerService.createCustomer(customer);
    }
}