package no.unit.nva.customer.service;

import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public interface CustomerService {

    CustomerDto getCustomer(UUID identifier) throws ApiGatewayException;

    CustomerDto getCustomerByOrgNumber(String orgNumber) throws ApiGatewayException;

    List<CustomerDto> getCustomers() throws ApiGatewayException;

    CustomerDto createCustomer(CustomerDto customer) throws ApiGatewayException;

    CustomerDto updateCustomer(UUID identifier, CustomerDto customer) throws ApiGatewayException;

    CustomerDto getCustomerByCristinId(String cristinId) throws ApiGatewayException;
}
