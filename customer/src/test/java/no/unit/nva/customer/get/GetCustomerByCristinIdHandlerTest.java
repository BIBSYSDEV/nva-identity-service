package no.unit.nva.customer.get;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class GetCustomerByCristinIdHandlerTest {
    
    public static final URI SAMPLE_CRISTIN_ID = CustomerDataGenerator.randomCristinOrgId();
    private GetCustomerByCristinIdHandler handler;
    private CustomerService customerService;
    private Context context;
    private ByteArrayOutputStream outputStream;
    
    @BeforeEach
    public void init() {
        customerService = mock(CustomerService.class);
        handler = new GetCustomerByCristinIdHandler(customerService);
        
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }
    
    @Test
    void handleRequestReturnsExistingCustomerOnValidCristinId()
        throws IOException, BadRequestException, NotFoundException {
        prepareMocksWithExistingCustomer();

        var encodedCristinOrgUrl = URLEncoder.encode(SAMPLE_CRISTIN_ID.toString(), StandardCharsets.UTF_8);
        Map<String, String> pathParameters = Map.of(GetCustomerByCristinIdHandler.CRISTIN_ID, encodedCristinOrgUrl);
        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withHeaders(getRequestHeaders())
                        .withPathParameters(pathParameters)
                        .build();
        
        var response = sendRequest(input, CustomerDto.class);
        
        var responseBody = CustomerDto.fromJson(response.getBody());
        
        assertNotNull(responseBody);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }
    
    @Test
    void handleRequestReturnsNotFoundOnInvalidCristinId() throws IOException, NotFoundException {
        prepareMocksWithMissingCustomer();
        
        Map<String, String> pathParameters = Map.of(GetCustomerByCristinIdHandler.CRISTIN_ID,
            SAMPLE_CRISTIN_ID.toString());
        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withHeaders(getRequestHeaders())
                        .withPathParameters(pathParameters)
                        .build();
        
        var response = sendRequest(input, Problem.class);
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode());
    }
    
    private <T> GatewayResponse<T> sendRequest(InputStream input, Class<T> responseType) throws IOException {
        handler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }
    
    private void prepareMocksWithMissingCustomer() throws NotFoundException {
        when(customerService.getCustomerByCristinId(SAMPLE_CRISTIN_ID)).thenThrow(NotFoundException.class);
    }
    
    private void prepareMocksWithExistingCustomer() throws NotFoundException {
        when(customerService.getCustomerByCristinId(SAMPLE_CRISTIN_ID)).thenReturn(createCustomer());
    }
    
    private CustomerDto createCustomer() {
        CustomerDao customer = new CustomerDao.Builder()
                                   .withIdentifier(UUID.randomUUID())
                                   .withCristinId(SAMPLE_CRISTIN_ID)
                                   .withCustomerOf(randomElement(ApplicationDomain.values()).getUri())
                                   .build();
        return customer.toCustomerDto();
    }
}
