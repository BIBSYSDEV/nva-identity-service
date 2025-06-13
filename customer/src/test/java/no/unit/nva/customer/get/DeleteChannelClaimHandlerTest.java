package no.unit.nva.customer.get;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.UUID.randomUUID;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_CHANNEL_CLAIMS;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
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
import no.unit.nva.customer.delete.DeleteChannelClaimHandler;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class DeleteChannelClaimHandlerTest {

    public static final Context CONTEXT = new FakeContext();
    private static final String IDENTIFIER = "identifier";
    private static final String CHANNEL_CLAIM_IDENTIFIER = "channelClaimIdentifier";
    private DeleteChannelClaimHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void init() {
        handler = new DeleteChannelClaimHandler(mock(CustomerService.class), new Environment());
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldThrowForbiddenWhenUserIsNotAuthorized() throws IOException {
        var request = createRequest(randomUUID(), randomUUID());

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldThrowBadRequestWhenProvidedChannelClaimIdentifierIsNotValid() throws IOException {
        var request = createAuthenticatedRequest(randomString(), randomUUID().toString(), MANAGE_CHANNEL_CLAIMS);

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldThrowBadRequestWhenProvidedCustomerIdentifierIsNotValid() throws IOException {
        var request = createAuthenticatedRequest(randomUUID().toString(), randomUUID().toString(), randomString(),
                                                 MANAGE_CHANNEL_CLAIMS);

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldThrowForbiddenWhenUserHasNoAccessRightToManageClaims() throws IOException {
        var request = createAuthenticatedRequest(randomUUID(), randomUUID(), MANAGE_DEGREE_EMBARGO);

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldThrowInternalServerErrorWhenUnexpectedFailureOccurs()
        throws IOException, InputException, NotFoundException {
        var request = createAuthenticatedRequest(randomUUID(), randomUUID(), MANAGE_CHANNEL_CLAIMS);

        new DeleteChannelClaimHandler(failingCustomerService(), new Environment()).handleRequest(request, output,
                                                                                                 CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_INTERNAL_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturnNoContentWhenChannelHasBeenSuccessfullyDeleted() throws IOException {
        var request = createAuthenticatedRequest(randomUUID(), randomUUID(), MANAGE_CHANNEL_CLAIMS);

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_NO_CONTENT, response.getStatusCode());
    }

    @Test
    void shouldReturnNoContentWhenChannelHasBeenSuccessfullyDeletedOnBehalfOfAnotherCustomer() throws IOException {
        var request = createAuthenticatedRequest(randomUUID(), randomUUID(), randomUUID(), MANAGE_CHANNEL_CLAIMS);

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_NO_CONTENT, response.getStatusCode());
    }

    private static CustomerService failingCustomerService() throws InputException, NotFoundException {
        var failingCustomerService = mock(CustomerService.class);
        doThrow(RuntimeException.class).when(failingCustomerService).deleteChannelClaim(any());
        return failingCustomerService;
    }

    private InputStream createAuthenticatedRequest(UUID channelClaimIdentifier,
                                                   UUID userAndChannelClaimCustomerIdentifier,
                                                   AccessRight accessRight)
        throws JsonProcessingException {
        return createAuthenticatedRequest(channelClaimIdentifier.toString(),
                                          userAndChannelClaimCustomerIdentifier.toString(),
                                          userAndChannelClaimCustomerIdentifier.toString(),
                                          accessRight);
    }

    private InputStream createAuthenticatedRequest(UUID channelClaimIdentifier, UUID usersCustomerIdentifier,
                                                   UUID channelClaimCustomerIdentifier, AccessRight accessRight)
        throws JsonProcessingException {
        return createAuthenticatedRequest(channelClaimIdentifier.toString(), usersCustomerIdentifier.toString(),
                                          channelClaimCustomerIdentifier.toString(), accessRight);
    }

    private InputStream createAuthenticatedRequest(String channelClaimIdentifier,
                                                   String userAndChannelClaimCustomerIdentifier,
                                                   AccessRight accessRight)
        throws JsonProcessingException {
        return createAuthenticatedRequest(channelClaimIdentifier, userAndChannelClaimCustomerIdentifier,
                                          userAndChannelClaimCustomerIdentifier, accessRight);
    }

    private InputStream createAuthenticatedRequest(String channelClaimIdentifier, String usersCustomerIdentifier,
                                                   String channelClaimCustomerIdentifier, AccessRight accessRight)
        throws JsonProcessingException {
        var customerId = UriWrapper.fromUri(randomUri()).addChild(usersCustomerIdentifier).getUri();
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withUserName(randomString())
                   .withCurrentCustomer(customerId)
                   .withAccessRights(customerId, accessRight)
                   .withPathParameters(Map.of(IDENTIFIER, channelClaimCustomerIdentifier, CHANNEL_CLAIM_IDENTIFIER,
                                              channelClaimIdentifier))
                   .build();
    }

    private InputStream createRequest(UUID channelClaimIdentifier, UUID customerIdentifier)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withPathParameters(
            Map.of(IDENTIFIER, customerIdentifier.toString(), CHANNEL_CLAIM_IDENTIFIER,
                   channelClaimIdentifier.toString())).build();
    }
}