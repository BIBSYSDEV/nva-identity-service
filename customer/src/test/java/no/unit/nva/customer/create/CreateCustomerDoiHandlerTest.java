package no.unit.nva.customer.create;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import no.unit.nva.customer.get.GetCustomerHandler;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateCustomerDoiHandlerTest extends LocalCustomerServiceDatabase {

    private CustomerService customerServiceMock;
    private CreateCustomerDoiHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        customerServiceMock = mock(CustomerService.class);
        handler = new CreateCustomerDoiHandler(customerServiceMock);

        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void processInput() {
    }
}