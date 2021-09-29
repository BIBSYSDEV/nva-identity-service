package no.unit.nva.customer.model;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import nva.commons.core.Environment;

public final class CustomerMapper {

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    public static final String NAMESPACE = new Environment().readEnv(ID_NAMESPACE_ENV);
    public static final URI NO_CONTEXT = null;
    public static final URI CONTEXT = URI.create("https://bibsysdev.github.io/src/customer-context.json");

    private CustomerMapper() {
    }

    public static CustomerDto addContext(CustomerDto customerDto) {
        CustomerDto withContext = customerDto.copy().build();
        URI id = toId(withContext.getIdentifier());
        withContext.setId(id);
        withContext.setContext(CONTEXT);
        return withContext;
    }

    /**
     * Map from Customer from Db to Dto version without context object.
     *
     * @param customerDb customerDb
     * @return customerDto
     */
    public static CustomerDto toCustomerDtoWithoutContext(CustomerDb customerDb) {
        CustomerDto customerDto = customerDb.toCustomerDto();
        customerDto.setContext(NO_CONTEXT);
        return customerDto;
    }

    public static CustomerDto removeContext(CustomerDto customerDto) {
        if (Objects.nonNull(customerDto)) {
            CustomerDto copy = customerDto.copy().build();
            copy.setContext(NO_CONTEXT);
            return copy;
        }
        return null;
    }

    public static URI toId(UUID identifier) {
        return URI.create(NAMESPACE + "/" + identifier);
    }
}
