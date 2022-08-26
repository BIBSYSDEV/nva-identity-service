package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.List;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;

public class NvaApplicationDomainHandler implements RequestHandler<Void, Object> {

    private final DynamoDBCustomerService customerService;

    public NvaApplicationDomainHandler(DynamoDBCustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public List<CustomerDto> handleRequest(Void input, Context context) {
        customerService.updateCustomersWithNvaAttribute();

        return customerService.getCustomers();
    }
}
