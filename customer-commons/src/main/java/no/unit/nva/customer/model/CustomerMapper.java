package no.unit.nva.customer.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto.Builder;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;

public class CustomerMapper {

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    public static final String NAMESPACE = getIdNamespace();
    public static final URI NO_CONTEXT = null;
    public static final URI CONTEXT = URI.create("https://bibsysdev.github.io/src/customer-context.json");
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public CustomerMapper(String namespace) {

    }

    public static CustomerDto addContext(CustomerDto customerDto) {
        return Optional.ofNullable(customerDto)
            .map(CustomerDto::copy)
            .map(copy -> copy.withContext(CONTEXT))
            .map(copy -> copy.withId(toId(customerDto.getIdentifier())))
            .map(Builder::build)
            .orElse(null);
    }

    public static URI toId(UUID identifier) {
        return URI.create(NAMESPACE + "/" + identifier);
    }

    /**
     * Map from Customer from Db to Dto version.
     *
     * @param customerDb customerDb
     * @return customerDto
     */
    public CustomerDto toCustomerDto(CustomerDb customerDb) {
        CustomerDto customerDto = objectMapper.convertValue(customerDb, CustomerDto.class);
        URI id = toId(customerDb.getIdentifier());
        customerDto.setId(id);
        customerDto.setContext(CONTEXT);
        return customerDto;
    }

    /**
     * Map from Customer from Db to Dto version without context object.
     *
     * @param customerDb customerDb
     * @return customerDto
     */
    public CustomerDto toCustomerDtoWithoutContext(CustomerDb customerDb) {
        CustomerDto customerDto = toCustomerDto(customerDb);
        customerDto.setContext(NO_CONTEXT);
        return customerDto;
    }

    /**
     * Map from list of Customers from Db to Dto version.
     *
     * @param customersDbs list of CustomerDb
     * @return customerList
     */
    public CustomerList toCustomerList(List<CustomerDb> customersDbs) {
        List<CustomerDto> customerDtos = customersDbs.stream()
            .map(this::toCustomerDtoWithoutContext)
            .collect(Collectors.toList()
            );
        return CustomerList.of(customerDtos);
    }

    /**
     * Map from Customer from Dto to Db version.
     *
     * @param customerDto customerDto
     * @return customerDb
     */
    public CustomerDb toCustomerDb(CustomerDto customerDto) {
        CustomerDb customer = objectMapper.convertValue(customerDto, CustomerDb.class);
        return customer;
    }

    private static String getIdNamespace() {
        return new Environment().readEnv(ID_NAMESPACE_ENV);
    }
}
