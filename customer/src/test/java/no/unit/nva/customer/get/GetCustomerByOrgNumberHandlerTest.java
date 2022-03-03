package no.unit.nva.customer.get;

import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetCustomerByOrgNumberHandlerTest {

    public static final String SAMPLE_ORG_NUMBER = "123";
    public static final String EXPECTED_ERROR_MESSAGE = "Missing from pathParameters: orgNumber";
    public static final String SAMPLE_CRISTIN_ID = "https://cristin.id";

    private CustomerService customerServiceMock;
    private GetCustomerByOrgNumberHandler handler;
    private Context context;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        customerServiceMock = mock(CustomerService.class);
        handler = new GetCustomerByOrgNumberHandler(customerServiceMock);
        context = Mockito.mock(Context.class);
    }

    @Test
    void getCustomerByOrgNumberReturnsCustomerWhenInputIsExistingCustomerOrgNumber() {
        UUID identifier = UUID.randomUUID();
        CustomerDao customerDb = new CustomerDao.Builder()
            .withIdentifier(identifier)
            .withFeideOrganizationId(SAMPLE_ORG_NUMBER)
            .withCristinId(SAMPLE_CRISTIN_ID)
            .build();
        CustomerDto customerDto = customerDb.toCustomerDto();
        when(customerServiceMock.getCustomerByOrgNumber(SAMPLE_ORG_NUMBER)).thenReturn(customerDto);

        var pathParameters = Map.of(GetCustomerByOrgNumberHandler.ORG_NUMBER, SAMPLE_ORG_NUMBER);
        var inputStream = createRequest(customerDto, pathParameters);
        var response = handler.handleRequest(inputStream, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getBody(), containsString(customerDto.getId().toString()));
        assertThat(response.getBody(), containsString(SAMPLE_CRISTIN_ID));
    }

    @Test
    void getCustomerByOrgNumberReturnsBadRequestWhenOrgNumberisNull() {

        var input = new APIGatewayProxyRequestEvent().withHeaders(getRequestHeaders());
        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(response.getBody(), containsString(EXPECTED_ERROR_MESSAGE));
    }

    private APIGatewayProxyRequestEvent createRequest(CustomerDto customerDto, Map<String, String> pathParameters) {
        return new APIGatewayProxyRequestEvent()
            .withBody(customerDto.toString())
            .withPathParameters(pathParameters)
            .withHeaders(getRequestHeaders());
    }
}
