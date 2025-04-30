package no.unit.nva.customer.get;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class DeleteChannelClaimHandlerTest extends LocalCustomerServiceDatabase {

    public static final Context CONTEXT = new FakeContext();
    private static final String IDENTIFIER = "identifier";
    private DeleteChannelClaimHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void init() {
        super.setupDatabase();
        CustomerService customerService = new DynamoDBCustomerService(dynamoClient);
        handler = new DeleteChannelClaimHandler(customerService, new Environment());
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldThrowForbiddenWhenUserHasNoAccessRightToManageClaims() throws IOException {
        var request = createRequest(UUID.randomUUID().toString());

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldThrowInternalServerErrorWhenUnexpectedFailureOccurs()
        throws IOException, InputException, NotFoundException {
        var request = createAuthenticatedRequest(UUID.randomUUID().toString());

        new DeleteChannelClaimHandler(failingCustomerService(), new Environment()).handleRequest(request, output,
                                                                                                 CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_INTERNAL_ERROR, response.getStatusCode());
    }

    @Test
    void shouldThrowBadRequestWhenProvidedChannelClaimIdentifierIsNotValid()
        throws IOException {
        var request = createAuthenticatedRequest(randomString());

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnNoContentWhenChannelHasBeenSuccessfullyDeleted() throws IOException {
        var request = createAuthenticatedRequest(UUID.randomUUID().toString());

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_NO_CONTENT, response.getStatusCode());
    }

    private static CustomerService failingCustomerService() throws InputException, NotFoundException {
        var failingCustomerService = mock(CustomerService.class);
        doThrow(RuntimeException.class).when(failingCustomerService).deleteChannelClaim(any());
        return failingCustomerService;
    }

    private InputStream createAuthenticatedRequest(String identifier) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .withPathParameters(Map.of(IDENTIFIER, identifier))
                   .withAccessRights(randomUri(), AccessRight.MANAGE_CHANNEL_CLAIMS)
                   .build();
    }

    private InputStream createRequest(String identifier) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withPathParameters(Map.of(IDENTIFIER, identifier))
                   .build();
    }
}