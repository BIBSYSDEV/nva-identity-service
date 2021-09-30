package no.unit.nva.customer.model;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto.Builder;
import nva.commons.core.Environment;

public final class CustomerMapper {

    public static final URI context = URI.create("https://bibsysdev.github.io/src/customer-context.json");
    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    public static final URI NAMESPACE = URI.create(getIdNamespace());
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

    private static String getIdNamespace() {
        return new Environment().readEnv(ID_NAMESPACE_ENV);
    }
}
