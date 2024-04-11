package no.unit.nva.customer;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerBatchScanHandler implements RequestHandler<Void, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerBatchScanHandler.class);
    public static final String CUSTOMER_REFRESHED_MESSAGE = "Customer {} with identifier {} have been refreshed";
    private final CustomerService customerService;

    @JacocoGenerated
    public CustomerBatchScanHandler() {
        this(defaultCustomerService());
    }

    public CustomerBatchScanHandler(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public Void handleRequest(Void input, Context context) {
        customerService.refreshCustomers().forEach(this::logMessage);
        return null;
    }

    private void logMessage(CustomerDto customerDto) {
        LOGGER.info(CUSTOMER_REFRESHED_MESSAGE, customerDto.getName(), customerDto.getIdentifier());
    }
}
