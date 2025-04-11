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
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
import nva.commons.core.Environment;
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
        handler = new ListAllChannelClaimsHandler(customerService, new Environment());
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
        new ListAllChannelClaimsHandler(customerServiceThrowingException, new Environment()).handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_BAD_GATEWAY));
    }

    @Test
    void shouldReturnOkWhenListingChannelClaims() throws IOException, ApiGatewayException {
        insertRandomCustomerWithChannelClaim(List.of(randomChannelClaimDto(), randomChannelClaimDto()));
        insertRandomCustomerWithChannelClaim(List.of(randomChannelClaimDto(), randomChannelClaimDto()));

        var request = createAuthorizedRequest();
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, ChannelClaimsListResponse.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));

        var channelClaimsListResponse = response.getBodyObject(ChannelClaimsListResponse.class);

        assertThat(channelClaimsListResponse.channelClaims().size(), is(4));
    }

    @Test
    void shouldReturnOkAndListChannelClaimsForInstitutionProvidedInQueryParam() throws IOException,
                                                                                       ApiGatewayException {
        var customer = insertRandomCustomerWithChannelClaim(randomChannelClaimDtos());
        insertRandomCustomerWithChannelClaim(List.of(randomChannelClaimDto(), randomChannelClaimDto()));

        var request = createAuthorizedRequestWithEncodedInstitutionInQueryParams(customer.getCristinId());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, ChannelClaimsListResponse.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));

        var channelClaimsListResponse = response.getBodyObject(ChannelClaimsListResponse.class);

        channelClaimsListResponse.channelClaims().forEach(channelClaimResponse ->
                         assertEquals(customer.getCristinId(), channelClaimResponse.claimedBy().organizationId()));
    }

    private CustomerDto insertRandomCustomerWithChannelClaim(List<ChannelClaimDto> channelClaims) throws ApiGatewayException {
        var customer = CustomerDto.builder()
                           .withDisplayName(randomString())
                           .withCristinId(randomUri())
                           .withCustomerOf(randomElement(ApplicationDomain.values()))
                           .withChannelClaims(channelClaims)
                           .build();
        return customerService.createCustomer(customer);
    }

    private static InputStream createAuthorizedRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withCurrentCustomer(randomUri())
                   .withUserName(randomString())
                   .build();
    }

    private static InputStream createAuthorizedRequestWithEncodedInstitutionInQueryParams(URI cristinId) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withCurrentCustomer(randomUri())
                   .withUserName(randomString())
                   .withTopLevelCristinOrgId(randomUri())
                   .withQueryParameters(Map.of("institution", URLEncoder.encode(cristinId.toString(), StandardCharsets.UTF_8)))
                   .build();
    }

    private static InputStream createUnauthorizedRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).build();
    }
}