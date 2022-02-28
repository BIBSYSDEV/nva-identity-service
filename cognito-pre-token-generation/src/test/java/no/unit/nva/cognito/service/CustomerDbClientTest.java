package no.unit.nva.cognito.service;

import static java.lang.String.join;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomerDbClientTest {

    public static final String SAMPLE_CRISTIN_ID = "http://example.org/cristin.id";
    public static final String SAMPLE_ORG_NUMBER = "123456789";
    public static final String SAMPLE_NAMESPACE = "http://example.org/namespace";
    public static final String DELIMITER = "/";

    private DynamoDBCustomerService dynamoDBCustomerService;
    private CustomerDbClient customerDbClient;

    @BeforeEach
    public void setUp() {
        //TODO: use DynamoDBCustomerService with DynamoDbLocal
        dynamoDBCustomerService = mock(DynamoDBCustomerService.class);
        customerDbClient = new CustomerDbClient(dynamoDBCustomerService);
    }

    @Test
    void getCustomerReturnsCustomerResponseIfCustomerExists() throws ApiGatewayException {
        UUID identifier = UUID.randomUUID();
        CustomerDao customerDb = getCustomerDb(identifier);
        when(dynamoDBCustomerService.getCustomerByOrgNumber(anyString())).thenReturn(customerDb);

        Optional<CustomerResponse> customer = customerDbClient.getCustomer(SAMPLE_ORG_NUMBER);

        assertThat(customer.isPresent(), is(true));
        assertThat(customer.get().getCustomerId().toString(),
                is(join(DELIMITER, SAMPLE_NAMESPACE, identifier.toString())));
        assertThat(customer.get().getCristinId(), is(SAMPLE_CRISTIN_ID));

    }

    @Test
    void getCustomerReturnsOptionalEmptyIfCustomerNotFound() throws ApiGatewayException {
        when(dynamoDBCustomerService.getCustomerByOrgNumber(anyString())).thenThrow(NotFoundException.class);

        Optional<CustomerResponse> customer = customerDbClient.getCustomer(SAMPLE_ORG_NUMBER);

        assertThat(customer.isPresent(), is(false));
    }

    private CustomerDao getCustomerDb(UUID identifier) {
        CustomerDao customerDb = new CustomerDao();
        customerDb.setIdentifier(identifier);
        customerDb.setCristinId(SAMPLE_CRISTIN_ID);
        return customerDb;
    }

}
