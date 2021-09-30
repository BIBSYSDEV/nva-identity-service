package no.unit.nva.customer.model;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto.Builder;
import nva.commons.core.Environment;

public final class CustomerMapper {

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    public static final String NAMESPACE = getIdNamespace();
    public static final URI NO_CONTEXT = null;
    public static final URI CONTEXT = URI.create("https://bibsysdev.github.io/src/customer-context.json");

    private CustomerMapper() {
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

    public static CustomerDto removeContext(CustomerDto customerDto) {
        if (Objects.nonNull(customerDto)) {
            CustomerDto copy = customerDto.copy().build();
            copy.setContext(NO_CONTEXT);
            return copy;
        }
        return null;
    }

    private static String getIdNamespace() {
        return new Environment().readEnv(ID_NAMESPACE_ENV);
    }
}
