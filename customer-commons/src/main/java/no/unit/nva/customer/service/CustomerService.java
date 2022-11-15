package no.unit.nva.customer.service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;

public interface CustomerService {

    CustomerDto getCustomer(URI id) throws NotFoundException;

    CustomerDto getCustomer(UUID identifier) throws NotFoundException;

    CustomerDto getCustomerByOrgDomain(String orgDomain) throws NotFoundException;

    List<CustomerDto> getCustomers();

    CustomerDto createCustomer(CustomerDto customer) throws NotFoundException, ConflictException;

    CustomerDto updateCustomer(UUID identifier, CustomerDto customer) throws InputException, NotFoundException;

    CustomerDto getCustomerByCristinId(URI cristinId) throws NotFoundException;

    DoiAgentDto getCustomerDoiAgentSecret(UUID identifier) throws NotFoundException;

    DoiAgentDto updateCustomerDoiAgentSecret(UUID identifier, DoiAgentDto doiAgent)
        throws NotFoundException, InputException;

    URL createCustomerDoi(UUID identifier) throws NotFoundException, MalformedURLException;
}
