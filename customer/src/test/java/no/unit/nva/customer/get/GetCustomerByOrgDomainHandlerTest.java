package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetCustomerByOrgDomainHandlerTest {

    public static final String SAMPLE_ORG_DOMAIN = "123";
    public static final URI SAMPLE_CRISTIN_ID = randomUri();

    private CustomerService customerServiceMock;
    private GetCustomerByOrgDomainHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        customerServiceMock = mock(CustomerService.class);
        handler = new GetCustomerByOrgDomainHandler(customerServiceMock);
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void getCustomerByOrgDomainReturnsCustomerWhenInputIsExistingCustomerOrgDomain()
            throws NotFoundException, IOException {
        UUID identifier = UUID.randomUUID();
        CustomerDao customerDb = new CustomerDao.Builder()
                .withIdentifier(identifier)
                .withFeideOrganizationDomain(SAMPLE_ORG_DOMAIN)
                .withCristinId(SAMPLE_CRISTIN_ID)
                .withCustomerOf(randomElement(ApplicationDomain.values()).getUri())
                .build();
        CustomerDto customerDto = customerDb.toCustomerDto();
        when(customerServiceMock.getCustomerByOrgDomain(SAMPLE_ORG_DOMAIN)).thenReturn(customerDto);

        var pathParameters = Map.of(GetCustomerByOrgDomainHandler.ORG_DOMAIN, SAMPLE_ORG_DOMAIN);
        var inputStream = createRequest(customerDto, pathParameters);
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, CustomerDto.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getBody(), containsString(customerDto.getId().toString()));
        assertThat(response.getBody(), containsString(SAMPLE_CRISTIN_ID.toString()));
    }

    private InputStream createRequest(CustomerDto customerDto, Map<String, String> pathParameters)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<CustomerDto>(dtoObjectMapper)
                .withBody(customerDto)
                .withPathParameters(pathParameters)
                .withHeaders(getRequestHeaders())
                .build();
    }
}
