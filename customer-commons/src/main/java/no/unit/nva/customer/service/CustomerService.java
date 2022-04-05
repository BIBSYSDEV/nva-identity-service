package no.unit.nva.customer.service;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;

public interface CustomerService {

    CustomerDto getCustomer(URI id);

    CustomerDto getCustomer(UUID identifier);

    CustomerDto getCustomerByOrgDomain(String orgDomain);

    List<CustomerDto> getCustomers();

    CustomerDto createCustomer(CustomerDto customer);

    CustomerDto updateCustomer(UUID identifier, CustomerDto customer);

    CustomerDto getCustomerByCristinId(URI cristinId);
}
