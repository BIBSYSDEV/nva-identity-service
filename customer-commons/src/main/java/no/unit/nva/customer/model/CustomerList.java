package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.customer.model.LinkedDataContextUtils.ID_NAMESPACE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.ShortMethodName")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class CustomerList {

    public static final String CUSTOMERS = "customers";

    @JsonProperty(CUSTOMERS)
    private final List<CustomerDtoWithoutContext> customers;

    @JsonCreator
    public CustomerList(@JsonProperty(CUSTOMERS) List<CustomerDto> customers) {
        this.customers = extractCustomers(customers);
    }

    @JsonProperty(LINKED_DATA_CONTEXT)
    public URI getContext() {
        return LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
    }

    @JsonProperty("id")
    public URI getId() {
        return ID_NAMESPACE;
    }

    public List<CustomerDtoWithoutContext> getCustomers() {
        return nonNull(customers) ? customers : Collections.emptyList();
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getCustomers(), getContext());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerList that = (CustomerList) o;
        return Objects.equals(getId(), that.getId())
               && Objects.equals(customers, that.customers)
               && Objects.equals(getContext(), that.getContext());
    }

    private List<CustomerDtoWithoutContext> extractCustomers(List<CustomerDto> customers) {
        return Optional.ofNullable(customers)
            .stream()
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .map(CustomerDto::withoutContext)
            .collect(Collectors.toList());
    }
}
