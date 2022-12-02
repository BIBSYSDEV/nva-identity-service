package no.unit.nva.customer.update;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateCustomerDoiHandlerTest extends LocalCustomerServiceDatabase {
    public static final Context CONTEXT = new FakeContext();
    private UpdateCustomerDoiHandler handler;
    private DynamoDBCustomerService customerService;
    private CustomerDto existingCustomer;
    private ByteArrayOutputStream outputStream;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void init() {
        super.setupDatabase();
        customerService = new DynamoDBCustomerService(dynamoClient);
        existingCustomer = attempt(CustomerDataGenerator::createSampleCustomerDto)
                               .map(customerInput -> customerService.createCustomer(customerInput))
                               .orElseThrow();
        handler = new UpdateCustomerDoiHandler(customerService);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void processInput() {
    }
}