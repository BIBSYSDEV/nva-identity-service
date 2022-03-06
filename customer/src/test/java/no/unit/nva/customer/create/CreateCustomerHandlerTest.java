package no.unit.nva.customer.create;

import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.customer.testing.TestHeaders.getResponseHeaders;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDao.Builder;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CreateCustomerHandlerTest {

    public static final String WILDCARD = "*";

    private CustomerService customerServiceMock;
    private CreateCustomerHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;

    /**
     * Setting up test environment.
     */
    @BeforeEach

    public void setUp() {
        customerServiceMock = mock(CustomerService.class);
        handler = new CreateCustomerHandler(customerServiceMock);
        outputStream = new ByteArrayOutputStream();
        context = new FakeContext();
    }

    @Test
    void requestToHandlerReturnsCustomerCreated() {
        var expectedBody = new Builder()
            .withName("New Customer")
            .withIdentifier(UUID.randomUUID())
            .withVocabularySettings(Collections.emptySet())
            .build()
            .toCustomerDto();
        when(customerServiceMock.createCustomer(any(CustomerDto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var input = new APIGatewayProxyRequestEvent()
            .withBody(expectedBody.toString())
            .withHeaders(getRequestHeaders());
        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        assertThat(response.getHeaders(), is(equalTo(getResponseHeaders())));

        var actualBody = CustomerDto.fromJson(response.getBody());
        assertThat(actualBody, is(equalTo(expectedBody)));
    }

    @Test
    void shouldReturnBadRequestWhenInputIsNotAValidCustomerDto() {
        var input = new APIGatewayProxyRequestEvent().withBody(randomString())
            .withHeaders(getRequestHeaders());
        var response = handler.handleRequest(input, context);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }
}
