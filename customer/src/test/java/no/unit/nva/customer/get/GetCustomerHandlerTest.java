package no.unit.nva.customer.get;

import static no.unit.nva.customer.get.GetCustomerHandler.IDENTIFIER;
import static no.unit.nva.customer.get.GetCustomerHandler.IDENTIFIER_IS_NOT_A_VALID_UUID;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigatewayv2.MediaTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetCustomerHandlerTest {

    public static final String MALFORMED_IDENTIFIER = "for-testing";
    public static final MediaType UNSUPPORTED_MEDIA_TYPE = MediaType.BZIP2;

    private CustomerService customerServiceMock;
    private GetCustomerHandler handler;
    private Context context;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        customerServiceMock = mock(CustomerService.class);
        handler = new GetCustomerHandler(customerServiceMock);

        context = Mockito.mock(Context.class);
    }

    @Test
    public void requestToHandlerWithJsonLdAcceptHeaderReturnsJsonLdMediaType() {
        UUID identifier = UUID.randomUUID();
        prepareServiceWithCustomer(identifier);

        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        var input = new APIGatewayProxyRequestEvent()
            .withHeaders(getRequestHeadersWithMediaType(MediaTypes.APPLICATION_JSON_LD))
            .withPathParameters(pathParameters);

        var response= handler.handleRequest(input, context);


        assertThat(response.getStatusCode(),is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE),
                   is(equalTo(MediaTypes.APPLICATION_JSON_LD.toString())));
    }

    @Test
    void requestToHandlerReturnsCustomer() {
        var identifier = UUID.randomUUID();
        var customerDto = prepareServiceWithCustomer(identifier);
        var inputStream = createGetCustomerRequest(customerDto);

        var response = handler.handleRequest(inputStream, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        CustomerDto actualCustomerDto = CustomerDto.fromJson(response.getBody());
        assertThat(actualCustomerDto.getId(), notNullValue());
        assertThat(actualCustomerDto.getContext(), notNullValue());
        assertThat(actualCustomerDto, equalTo(customerDto));
    }

    @Test
    void requestToHandlerWithMalformedIdentifierReturnsBadRequest() {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, MALFORMED_IDENTIFIER);
        var input = new APIGatewayProxyRequestEvent()
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters);

        var response = handler.handleRequest(input, context);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(response.getBody(), containsString(IDENTIFIER_IS_NOT_A_VALID_UUID + MALFORMED_IDENTIFIER));
    }

    @Test
    void requestToHandlerWithUnsupportedAcceptHeaderReturnsUnsupportedMediaType() {
        UUID identifier = UUID.randomUUID();
        prepareServiceWithCustomer(identifier);

        Map<String, String> pathParameters = Map.of(IDENTIFIER, identifier.toString());
        var input = new APIGatewayProxyRequestEvent()
            .withHeaders(getRequestHeadersWithUnsupportedMediaType())
            .withPathParameters(pathParameters);

        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNSUPPORTED_TYPE )));
    }

    private static Map<String, String> getRequestHeadersWithUnsupportedMediaType() {
        return Map.of(
            HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
            HttpHeaders.ACCEPT, UNSUPPORTED_MEDIA_TYPE.toString()
        );
    }

    private static Map<String, String> getRequestHeadersWithMediaType(MediaType mediaType) {
        return Map.of(
            HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
            HttpHeaders.ACCEPT, mediaType.toString()
        );
    }

    private APIGatewayProxyRequestEvent createGetCustomerRequest(CustomerDto customerDto) {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, customerDto.getIdentifier().toString());
        return new APIGatewayProxyRequestEvent()
            .withBody(customerDto.toString())
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters);
    }

    private CustomerDto prepareServiceWithCustomer(UUID identifier) {
        CustomerDao customerDb = new CustomerDao.Builder()
            .withIdentifier(identifier)
            .build();
        CustomerDto customerDto = customerDb.toCustomerDto();
        when(customerServiceMock.getCustomer(identifier)).thenReturn(customerDto);
        return customerDto;
    }
}
