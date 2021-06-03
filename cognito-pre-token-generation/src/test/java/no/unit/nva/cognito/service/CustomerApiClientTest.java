package no.unit.nva.cognito.service;

import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Optional;
import no.unit.nva.cognito.model.CustomerResponse;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

@SuppressWarnings("unchecked")
public class CustomerApiClientTest {

    public static final String HTTP = "http";
    public static final String EXAMPLE_ORG = "example.org";
    public static final String ORG_NUMBER = "orgNumber";
    public static final String GARBAGE_JSON = "{{}";
    public static final String SAMPLE_ID = "http://link.to.id";
    public static final String CRISTIN_ID = "cristinId";
    public static final String CUSTOMER_ID = "customerId";

    public static final String RESPONSE_TEMPLATE = "{\"%s\":\"%s\"}";
    public static final Object NO_BODY = null;
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    private CustomerApiClient customerApiClient;
    private HttpClient httpClient;
    private HttpResponse httpResponse;

    /**
     * Set up test environment.
     */
    @BeforeEach
    public void init() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(CustomerApiClient.CUSTOMER_API_SCHEME)).thenReturn(HTTP);
        when(environment.readEnv(CustomerApiClient.CUSTOMER_API_HOST)).thenReturn(EXAMPLE_ORG);
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);

        customerApiClient = new CustomerApiClient(httpClient, new ObjectMapper(), environment);
    }

    @Test
    public void getCustomerReturnsCustomerIdentifierOnInput() throws IOException, InterruptedException {
        when(httpResponse.body()).thenReturn(generateValidCustomerResponse(CUSTOMER_ID));
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<CustomerResponse> customer = customerApiClient.getCustomer(ORG_NUMBER);

        assertEquals(SAMPLE_ID, customer.get().getCustomerId());
    }

    @Test
    public void getCustomerReturnsCristinIdOnInput() throws IOException, InterruptedException {
        when(httpResponse.body()).thenReturn(generateValidCustomerResponse(CRISTIN_ID));
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<CustomerResponse> customer = customerApiClient.getCustomer(ORG_NUMBER);

        assertEquals(SAMPLE_ID, customer.get().getCristinId());
    }

    @Test
    public void getCustomerThrowsIllegalStateExceptionOnInvalidJsonResponse() throws IOException, InterruptedException {
        when(httpResponse.body()).thenReturn(GARBAGE_JSON);
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        var exception = assertThrows(IllegalStateException.class, () -> customerApiClient
            .getCustomer(ORG_NUMBER));
        assertThat(exception.getMessage(), is(equalTo("Error parsing customer information")));
    }

    @Test
    public void getCustomerThrowsIllegalStateExceptionOnNonSuccessfullHttpResponse()
        throws IOException, InterruptedException {
        when(httpResponse.statusCode()).thenReturn(SC_BAD_GATEWAY);
        when(httpResponse.body()).thenReturn(NO_BODY);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        var exception = assertThrows(IllegalStateException.class, () -> customerApiClient
            .getCustomer(ORG_NUMBER));
        assertThat(exception.getMessage(), is(equalTo("Error fetching customer information")));
    }

    @Test
    public void getCustomerThrowsIllegalStateExceptionWhenHttpClientThrowsException()
        throws IOException, InterruptedException {
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        var exception = assertThrows(IllegalStateException.class, () -> customerApiClient
            .getCustomer(ORG_NUMBER));
        assertThat(exception.getMessage(),
            is(equalTo("Error fetching customer information, http client failed to initialize.")));
    }

    @Test
    public void getCustomerWithSuccessfullyParsedCustomerResponseReturnsEmptyOptional()
        throws IOException, InterruptedException {
        when(httpResponse.body()).thenReturn(notFoundProblemResponse());
        when(httpResponse.statusCode()).thenReturn(SC_NOT_FOUND);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<CustomerResponse> customer = customerApiClient.getCustomer(ORG_NUMBER);

        assertTrue(customer.isEmpty());
    }

    private String generateValidCustomerResponse(String identifier) {
        return String.format(RESPONSE_TEMPLATE, identifier, SAMPLE_ID);
    }

    private String notFoundProblemResponse() throws JsonProcessingException {
        return objectMapper.writeValueAsString(Problem.builder()
            .withStatus(Status.NOT_FOUND)
            .withDetail("Customer not found: " + ORG_NUMBER)
            .build());
    }
}
