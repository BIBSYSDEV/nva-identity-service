package no.unit.nva.customer.create;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDto;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelClaimDto;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelConstraintDto;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_CHANNEL_CLAIMS;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintDto;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class CreateChannelClaimHandlerTest extends LocalCustomerServiceDatabase {

    private static final String IDENTIFIER_PATH_PARAMETER = "identifier";
    private CreateChannelClaimHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;
    private DynamoDBCustomerService customerService;
    private CustomerDto existingCustomer;

    @BeforeEach
    public void setUp() throws ApiGatewayException {
        super.setupDatabase();
        this.customerService = new DynamoDBCustomerService(this.dynamoClient);
        existingCustomer = customerService.createCustomer(createSampleCustomerDto());
        handler = new CreateChannelClaimHandler(customerService, new Environment());
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    public void close() {
        super.deleteDatabase();
    }

    @Test
    void shouldReturnCreatedWhenCreatingChannelClaim() throws IOException {
        var request = createValidRequest();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, ChannelClaimDto.class);
        assertEquals(HttpURLConnection.HTTP_CREATED, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenInvalidChannelUriIsProvidedInRequest() throws IOException {
        var request = createRequestWithInvalidChannelUriInBody();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenCustomerDoesNotExist() throws IOException {
        var request = createRequestWithNonExistingCustomer();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingChannelClaimForAnotherCustomer()
        throws IOException, ConflictException, NotFoundException {
        var request = createRequestWithUserNotBelongingToCustomer();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingChannelClaimWithoutAccessRight() throws IOException {
        var request = createRequestWithoutAccessRights();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingChannelClaimWithWrongAccessRight() throws IOException {
        var request = createRequestWithWrongAccessRight();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldReturnConflictWhenClaimingChannelAlreadyClaimedByCustomer() throws IOException {
        var request = createRequestClaimingChannelAlreadyClaimedByCustomer();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(HttpURLConnection.HTTP_CONFLICT, response.getStatusCode());
    }

    @Test
    void shouldReturnConflictWhenClaimingChannelAlreadyClaimedByAnotherCustomer()
        throws IOException, ConflictException, NotFoundException {
        var request = createRequestClaimingChannelClaimedByAnotherCustomer();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(HttpURLConnection.HTTP_CONFLICT, response.getStatusCode());
    }

    @Test
    void shouldReturnInternalErrorWhenUnexpectedError()
        throws IOException, InputException, NotFoundException, BadRequestException, ConflictException {
        var request = createValidRequest();
        var customerService = mock(CustomerService.class);
        doThrow(new IllegalStateException()).when(customerService).createChannelClaim(any(), any());
        var handler = new CreateChannelClaimHandler(customerService, new Environment());

        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, response.getStatusCode());
        assertEquals("Internal server error. Contact application administrator.",
                     response.getBodyObject(Problem.class).getDetail());
    }

    private HandlerRequestBuilder<ChannelClaimRequest> createDefaultRequestBuilder(UUID customerToClaim,
                                                                                   ChannelClaimRequest channelClaim)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<ChannelClaimRequest>(dtoObjectMapper)
                   .withPathParameters(Map.of(IDENTIFIER_PATH_PARAMETER, customerToClaim.toString()))
                   .withCurrentCustomer(existingCustomer.getId())
                   .withBody(channelClaim);
    }

    private InputStream createValidRequest() throws JsonProcessingException {
        return createDefaultRequestBuilder(existingCustomer.getIdentifier(), randomChannelClaimRequest())
                   .withAccessRights(existingCustomer.getId(), MANAGE_CHANNEL_CLAIMS)
                   .build();
    }

    private InputStream createRequestClaimingChannelAlreadyClaimedByCustomer() throws JsonProcessingException {
        var existingClaim = existingCustomer.getChannelClaims().stream().findFirst().orElseThrow();
        return createDefaultRequestBuilder(existingCustomer.getIdentifier(), fromDto(existingClaim))
                   .withAccessRights(existingCustomer.getId(), MANAGE_CHANNEL_CLAIMS)
                   .build();
    }

    private InputStream createRequestClaimingChannelClaimedByAnotherCustomer()
        throws ConflictException, NotFoundException, JsonProcessingException {
        var anotherCustomer = customerService.createCustomer(createSampleCustomerDto());
        var existingClaimByAnotherCustomer = anotherCustomer.getChannelClaims().stream().findFirst().orElseThrow();
        return createDefaultRequestBuilder(existingCustomer.getIdentifier(), fromDto(existingClaimByAnotherCustomer))
                   .withAccessRights(existingCustomer.getId(), MANAGE_CHANNEL_CLAIMS)
                   .build();
    }

    private InputStream createRequestWithNonExistingCustomer() throws JsonProcessingException {
        var nonExistingCustomerIdentifier = UUID.randomUUID();
        var nonExistingCustomerId = UriWrapper.fromHost(randomString())
                                        .addChild(nonExistingCustomerIdentifier.toString())
                                        .getUri();
        return createDefaultRequestBuilder(nonExistingCustomerIdentifier, randomChannelClaimRequest())
                   .withCurrentCustomer(nonExistingCustomerId)
                   .withAccessRights(existingCustomer.getId(), MANAGE_CHANNEL_CLAIMS)
                   .build();
    }

    private InputStream createRequestWithUserNotBelongingToCustomer()
        throws JsonProcessingException, ConflictException, NotFoundException {
        var anotherCustomer = customerService.createCustomer(createSampleCustomerDto());
        return createDefaultRequestBuilder(anotherCustomer.getIdentifier(), randomChannelClaimRequest())
                   .withAccessRights(existingCustomer.getId(), MANAGE_CHANNEL_CLAIMS)
                   .build();
    }

    private InputStream createRequestWithoutAccessRights() throws JsonProcessingException {
        return createDefaultRequestBuilder(existingCustomer.getIdentifier(), randomChannelClaimRequest())
                   .build();
    }

    private InputStream createRequestWithWrongAccessRight() throws JsonProcessingException {
        return createDefaultRequestBuilder(existingCustomer.getIdentifier(), randomChannelClaimRequest())
                   .withAccessRights(existingCustomer.getId(), MANAGE_DOI)
                   .build();
    }

    private InputStream createRequestWithInvalidChannelUriInBody() throws JsonProcessingException {
        var channelClaimRequest = new ChannelClaimRequest(randomUri(), fromDto(randomChannelConstraintDto()));
        return createDefaultRequestBuilder(existingCustomer.getIdentifier(), channelClaimRequest)
                   .withAccessRights(existingCustomer.getId(), MANAGE_CHANNEL_CLAIMS)
                   .build();
    }

    private ChannelClaimRequest randomChannelClaimRequest() {
        return fromDto(randomChannelClaimDto());
    }

    private ChannelClaimRequest fromDto(ChannelClaimDto dto) {
        return new ChannelClaimRequest(dto.channel(), fromDto(dto.constraint()));
    }

    private ChannelConstraintRequest fromDto(ChannelConstraintDto dto) {
        return new ChannelConstraintRequest(dto.publishingPolicy(), dto.editingPolicy(), dto.scope());
    }
}