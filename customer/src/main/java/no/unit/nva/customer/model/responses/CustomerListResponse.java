package no.unit.nva.customer.model.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.LinkedDataContextUtils;
import no.unit.nva.customer.model.interfaces.WithContext;
import no.unit.nva.customer.model.interfaces.WithId;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonTypeName("CustomerList")
public class CustomerListResponse implements WithId, WithContext {

    private List<CustomerDto> customers;
    private URI id;
    @JsonProperty("@context")
    private URI context;

    public CustomerListResponse() {
        this(Collections.emptyList());
    }

    public CustomerListResponse(List<CustomerDto> customers) {
        this.customers = customers;
        this.id = LinkedDataContextUtils.ID_NAMESPACE;
        this.context = LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
    }

    public List<CustomerDto> getCustomers() {
        return customers;
    }

    public void setCustomers(List<CustomerDto> customers) {
        this.customers = customers;
    }

    @Override
    public URI getId() {
        return id;
    }

    @Override
    public void setId(URI id) {
        this.id = id;
    }

    @Override
    public URI getContext() {
        return context;
    }

    @Override
    public void setContext(URI context) {
        this.context = context;
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
        CustomerListResponse that = (CustomerListResponse) o;
        return Objects.equal(getCustomers(), that.getCustomers())
                && Objects.equal(getId(), that.getId())
                && Objects.equal(getContext(), that.getContext());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hashCode(getCustomers(), getId(), getContext());
    }
}
