package no.unit.nva.customer.model;

import static no.unit.nva.customer.model.CustomerMapper.NAMESPACE;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.ShortMethodName")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class CustomerList {

    private URI id;
    private List<CustomerDtoWithoutContext> customers;
    @JsonProperty("@context")
    private URI context;

    @JacocoGenerated
    public CustomerList() {

    }

    public CustomerList(List<CustomerDto> customers) {
        this.id = NAMESPACE;
        this.customers = extractCustomers(customers);
        this.context = CustomerMapper.CONTEXT;
    }

    public List<CustomerDtoWithoutContext> getCustomers() {
        return customers;
    }

    public void setCustomers(List<CustomerDtoWithoutContext> customers) {
        this.customers = customers;
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public URI getContext() {
        return context;
    }

    public void setContext(URI context) {
        this.context = context;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(id, customers, context);
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
        return Objects.equals(id, that.id)
               && Objects.equals(customers, that.customers)
               && Objects.equals(context, that.context);
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
