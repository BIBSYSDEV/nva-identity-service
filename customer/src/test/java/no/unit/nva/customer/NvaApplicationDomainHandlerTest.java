package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.IOException;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NvaApplicationDomainHandlerTest extends LocalCustomerServiceDatabase {

    private DynamoDBCustomerService customerService;
    private NvaApplicationDomainHandler handler;
    private Context context;
    private Void input;


    @BeforeEach
    public void setUp() {
        customerService = new DynamoDBCustomerService(this.dynamoClient);
        handler = new NvaApplicationDomainHandler(customerService);
        context = new FakeContext();

    }

    @Test
    void shouldHandleRequest() {
        handler.handleRequest(input, context);
    }
}
