package no.unit.nva.customer.model;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto.Builder;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public final class LinkedDataContextUtils {

    public static final URI ID_NAMESPACE = URI.create(getIdNamespace());

    public static final String LINKED_DATA_ID = "id";
    public static final String LINKED_DATA_CONTEXT = "@context";
    public static final URI LINKED_DATA_CONTEXT_VALUE =
        URI.create("https://bibsysdev.github.io/src/customer-context.json");

    private LinkedDataContextUtils() {
    }

    public static CustomerDto addContext(CustomerDto customerDto) {
        return Optional.ofNullable(customerDto)
            .map(CustomerDto::copy)
            .map(copy -> copy.withContext(LINKED_DATA_CONTEXT_VALUE))
            .map(copy -> copy.withId(toId(customerDto.getIdentifier())))
            .map(Builder::build)
            .orElse(null);
    }

    public static URI toId(UUID identifier) {
        return new UriWrapper(ID_NAMESPACE).addChild(identifier.toString()).getUri();
    }

    private static String getIdNamespace() {
        return new Environment().readEnv("ID_NAMESPACE");
    }
}
