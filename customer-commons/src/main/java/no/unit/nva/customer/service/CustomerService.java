package no.unit.nva.customer.service;

import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public interface CustomerService {

    CustomerDto getCustomer(URI id) throws NotFoundException;

    CustomerDto getCustomer(UUID identifier) throws NotFoundException;

    CustomerDto getCustomerByOrgDomain(String orgDomain) throws NotFoundException;

    List<CustomerDto> getCustomers();

    CustomerDto createCustomer(CustomerDto customer) throws NotFoundException, ConflictException;

    CustomerDto updateCustomer(UUID identifier, CustomerDto customer) throws InputException, NotFoundException;

    CustomerDto getCustomerByCristinId(URI cristinId) throws NotFoundException;

    List<CustomerDto> refreshCustomers();

}
