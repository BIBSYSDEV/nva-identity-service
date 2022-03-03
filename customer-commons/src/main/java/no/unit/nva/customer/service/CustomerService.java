package no.unit.nva.customer.service;

import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;

public interface CustomerService {

    CustomerDto getCustomer(UUID identifier);

    CustomerDto getCustomerByOrgNumber(String orgNumber);

    List<CustomerDto> getCustomers();

    CustomerDto createCustomer(CustomerDto customer);

    CustomerDto updateCustomer(UUID identifier, CustomerDto customer);

    CustomerDto getCustomerByCristinId(String cristinId);
}
