package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.core.JacocoGenerated;

public class NvaApplicationDomainHandler implements RequestHandler<Void, Object> {

    private final DynamoDBCustomerService customerService;

    @JacocoGenerated
    public NvaApplicationDomainHandler(DynamoDBCustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public Void handleRequest(Void input, Context context) {
        customerService.addCustomerOfNvaAttribute();
        return null;
    }
}
