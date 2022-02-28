package no.unit.nva.customer.service;

import java.util.List;
import java.util.UUID;

import no.unit.nva.customer.model.CustomerDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public interface CustomerService {

    CustomerDao getCustomer(UUID identifier) throws ApiGatewayException;

    CustomerDao getCustomerByOrgNumber(String orgNumber) throws ApiGatewayException;

    List<CustomerDao> getCustomers() throws ApiGatewayException;

    CustomerDao createCustomer(CustomerDao customer) throws ApiGatewayException;

    CustomerDao updateCustomer(UUID identifier, CustomerDao customer) throws ApiGatewayException;

    CustomerDao getCustomerByCristinId(String cristinId) throws ApiGatewayException;
}
