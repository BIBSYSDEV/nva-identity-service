package no.unit.nva.customer.get;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelClaimDto;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelClaimDtos;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.customer.get.response.ChannelClaimsListResponse;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class ListAllChannelClaimsHandlerTest extends LocalCustomerServiceDatabase {

    public static final Context CONTEXT = new FakeContext();
    private ListAllChannelClaimsHandler handler;
    private CustomerService customerService;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void init() {
        super.setupDatabase();
        customerService = new DynamoDBCustomerService(dynamoClient);
        handler = new ListAllChannelClaimsHandler(customerService);
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldThrowUnauthorizedWhenUserNotLoggedIn() throws IOException {
        var request = createUnauthorizedRequest();

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_UNAUTHORIZED));
    }

    @Test
    void shouldThrowBadGatewayWhenUnexpectedErrorOccurs() throws IOException {
        var request = createAuthorizedRequest();
        var customerServiceThrowingException = mock(CustomerService.class);

        when(customerServiceThrowingException.getChannelClaims()).thenThrow(RuntimeException.class);
        new ListAllChannelClaimsHandler(customerServiceThrowingException).handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_BAD_GATEWAY));
    }

    @Test
    void shouldReturnOkWhenListingChannelClaims() throws IOException, ApiGatewayException {
        var expectedChannelClaim = randomChannelClaimDto();
        insertRandomCustomerWithChannelClaim(List.of(expectedChannelClaim));

        var request = createAuthorizedRequest();
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, ChannelClaimsListResponse.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));

        var body = response.getBodyObject(ChannelClaimsListResponse.class);
        var actualChannelClaim = body.channelClaims().stream().findFirst().orElseThrow();
        assertEquals(expectedChannelClaim.channel(), actualChannelClaim.channelClaim().channel());
    }

    private void insertRandomCustomerWithChannelClaim(List<ChannelClaimDto> channelClaims) throws ApiGatewayException {
        var customer = CustomerDto.builder()
                           .withDisplayName(randomString())
                           .withCristinId(randomUri())
                           .withCustomerOf(randomElement(ApplicationDomain.values()))
                           .withChannelClaims(channelClaims)
                           .build();
        customerService.createCustomer(customer);
    }

    private static InputStream createAuthorizedRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withCurrentCustomer(randomUri())
                   .withUserName(randomString())
                   .build();
    }

    private static InputStream createUnauthorizedRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).build();
    }
}