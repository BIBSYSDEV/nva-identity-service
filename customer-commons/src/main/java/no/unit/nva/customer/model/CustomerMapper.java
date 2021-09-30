package no.unit.nva.customer.model;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.customer.model.CustomerDto.Builder;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;

public final class CustomerMapper {

    public static final URI context = URI.create("https://bibsysdev.github.io/src/customer-context.json");
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    public static final String NAMESPACE = getIdNamespace();
    public static final URI NO_CONTEXT = null;
    public static final URI CONTEXT = URI.create("https://bibsysdev.github.io/src/customer-context.json");

    private CustomerMapper() {
    }

    private static CustomerDtoWithoutContext toCustomerDtoWithoutContext(CustomerDto customerDto) {
        return objectMapper.convertValue(customerDto, CustomerDtoWithoutContext.class);
    }

    public static CustomerList toCustomerListFromCustomerDtosWithoutContexts(
            List<CustomerDtoWithoutContext> customerDtos) {
        URI id = URI.create(getIdNamespace());
        return new CustomerList(id, customerDtos);
    }

    public static CustomerList toCustomerListFromCustomerDtos(List<CustomerDto> customerDtos) {
        List<CustomerDtoWithoutContext> customerDtosWithoutContexts = customerDtos.stream()
                .map(CustomerMapper::toCustomerDtoWithoutContext)
                .collect(Collectors.toList());
        return toCustomerListFromCustomerDtosWithoutContexts(customerDtosWithoutContexts);
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

    private static String getIdNamespace() {
        return new Environment().readEnv(ID_NAMESPACE_ENV);
    }
}
