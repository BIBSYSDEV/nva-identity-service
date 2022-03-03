package no.unit.nva.customer.get;

import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import no.unit.nva.customer.testing.CustomerDataGenerator;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetCustomerByCristinIdHandlerTest {

    public static final String SAMPLE_CRISTIN_ID = CustomerDataGenerator.randomCristinOrgId().toString();
    private GetCustomerByCristinIdHandler handler;
    private CustomerService customerService;
    private Context context;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void init() {
        customerService = mock(CustomerService.class);
        handler = new GetCustomerByCristinIdHandler(customerService);

        context = Mockito.mock(Context.class);
    }

    @Test
    public void handleRequestReturnsExistingCustomerOnValidCristinId() throws Exception {
        prepareMocksWithExistingCustomer();

        Map<String, String> pathParameters = Map.of(GetCustomerByCristinIdHandler.CRISTIN_ID, SAMPLE_CRISTIN_ID);
        var input = new APIGatewayProxyRequestEvent()
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters);

        var response =handler.handleRequest(input, context);

        var responseBody = CustomerDto.fromJson(response.getBody());

        assertNotNull(responseBody);
        assertThat(response.getStatusCode(),is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    public void handleRequestReturnsNotFoundOnInvalidCristinId() throws Exception {
        prepareMocksWithMissingCustomer();

        Map<String, String> pathParameters = Map.of(GetCustomerByCristinIdHandler.CRISTIN_ID, SAMPLE_CRISTIN_ID);
        var input= new APIGatewayProxyRequestEvent()
            .withHeaders(getRequestHeaders())
            .withPathParameters(pathParameters);

        var response=  handler.handleRequest(input, context);
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode());
    }

    private void prepareMocksWithMissingCustomer() {
        when(customerService.getCustomerByCristinId(SAMPLE_CRISTIN_ID)).thenThrow(NotFoundException.class);
    }

    private void prepareMocksWithExistingCustomer()  {
        when(customerService.getCustomerByCristinId(SAMPLE_CRISTIN_ID)).thenReturn(createCustomer());
    }

    private CustomerDto createCustomer() {
        CustomerDao customer = new CustomerDao.Builder()
            .withIdentifier(UUID.randomUUID())
            .withCristinId(SAMPLE_CRISTIN_ID)
            .build();
        return customer.toCustomerDto();
    }
}
