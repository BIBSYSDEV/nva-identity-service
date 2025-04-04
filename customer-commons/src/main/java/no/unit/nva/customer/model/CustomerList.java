package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static no.unit.nva.customer.Constants.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.Constants.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.Constants.LINKED_DATA_ID;
import static nva.commons.core.attempt.Try.attempt;

@SuppressWarnings("PMD.ShortMethodName")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class CustomerList {

    public static final String CUSTOMERS = "customers";
    @JsonProperty(CUSTOMERS)
    private List<CustomerReference> customers;

    public CustomerList() {

    }

    public CustomerList(List<CustomerDto> customers) {
        this.customers = extractCustomers(customers);
    }

    private List<CustomerReference> extractCustomers(List<CustomerDto> customers) {
        return Optional.ofNullable(customers)
            .stream()
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .map(CustomerReference::fromCustomerDto)
            .collect(Collectors.toList());
    }

    public static CustomerList fromString(String json) {
        return attempt(() -> JsonConfig.readValue(json, CustomerList.class)).orElseThrow();
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getCustomers(), getContext());
    }

    @JsonProperty(LINKED_DATA_CONTEXT)
    public URI getContext() {
        return LINKED_DATA_CONTEXT_VALUE;
    }

    @JsonProperty("id")
    public URI getId() {
        return URI.create(new Environment().readEnv("ID_NAMESPACE"));
    }

    public List<CustomerReference> getCustomers() {
        return nonNull(customers) ? customers : Collections.emptyList();
    }

    public void setCustomers(List<CustomerReference> customers) {
        this.customers = customers;
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

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
