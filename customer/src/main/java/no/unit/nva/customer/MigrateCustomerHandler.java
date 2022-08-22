package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;

public class MigrateCustomerHandler implements RequestHandler<Object, Object> {

    private CustomerService customerService;

    public MigrateCustomerHandler(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public Object handleRequest(Object input, Context context) {
        var customers = customerService.getCustomers();
        attachCustomerOfDomainAttributeToCustomers(customers);
        return null;
    }

    public List<CustomerDto> attachCustomerOfDomainAttributeToCustomers(List<CustomerDto> customers) {
        return customers.stream().map(customer -> customer.setCustomerOf(ApplicationDomain.NVA)).;
    }
}
