package no.unit.nva.customer.service;

import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDb;
import nva.commons.exceptions.ApiGatewayException;

public interface CustomerService {

    CustomerDb getCustomer(UUID identifier) throws ApiGatewayException;

    CustomerDb getCustomerByOrgNumber(String orgNumber) throws ApiGatewayException;

    List<CustomerDb> getCustomers() throws ApiGatewayException;

    CustomerDb createCustomer(CustomerDb customer) throws ApiGatewayException;

    CustomerDb updateCustomer(UUID identifier, CustomerDb customer) throws ApiGatewayException;

    CustomerDb getCustomerByCristinId(String cristinId) throws ApiGatewayException;
}
