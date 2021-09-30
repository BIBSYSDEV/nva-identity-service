package no.unit.nva.customer.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomerMapper {

    public static final URI context = URI.create("https://bibsysdev.github.io/src/customer-context.json");
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    private final String namespace;

    public CustomerMapper(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Map from Customer from Db to Dto version.
     *
     * @param customerDb    customerDb
     * @return  customerDto
     */
    public CustomerDto toCustomerDto(CustomerDb customerDb) {
        CustomerDto customerDto = objectMapper.convertValue(customerDb, CustomerDto.class);
        URI id = toId(customerDb.getIdentifier());
        customerDto.setId(id);
        customerDto.setContext(context);
        return customerDto;
    }

    private CustomerDtoWithoutContext toCustomerDtoWithoutContext(CustomerDto customerDto) {
        return objectMapper.convertValue(customerDto, CustomerDtoWithoutContext.class);
    }

    /**
     * Map from list of Customers from Db to Dto version.
     *
     * @param customerDbs  list of CustomerDb
     * @return  customerList
     */
    public CustomerList toCustomerListFromCustomerDbs(List<CustomerDb> customerDbs) {
        List<CustomerDtoWithoutContext> customerDtos = customerDbs.stream()
            .map(this::toCustomerDto)
            .map(this::toCustomerDtoWithoutContext)
            .collect(Collectors.toList());
        return toCustomerListFromCustomerDtos(customerDtos);
    }

    /**
     * Map from list of Customers from Db to Dto version.
     *
     * @param customerDtos  list of CustomerDto
     * @return  customerList
     */
    public CustomerList toCustomerListFromCustomerDtos(List<CustomerDtoWithoutContext> customerDtos) {
        URI id = URI.create(namespace);
        return new CustomerList(id, customerDtos);
    }

    /**
     * Map from Customer from Dto to Db version.
     *
     * @param customerDto   customerDto
     * @return  customerDb
     */
    public CustomerDb toCustomerDb(CustomerDto customerDto) {
        CustomerDb customer = objectMapper.convertValue(customerDto, CustomerDb.class);
        return customer;
    }

    private URI toId(UUID identifier) {
        return URI.create(namespace + "/" + identifier);
    }

}
