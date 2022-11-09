package no.unit.nva.customer.model;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class CustomerReference {

    private URI id;
    private String displayName;
    private Instant createdDate;

    public static CustomerReference fromCustomerDto(CustomerDto customerDto) {
        var customerReference = new CustomerReference();
        customerReference.setDisplayName(customerDto.getDisplayName());
        customerReference.setId(customerDto.getId());
        customerReference.setCreatedDate(customerDto.getCreatedDate());
        return customerReference;
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCreatedDate() {
        return (createdDate == null) ? null : createdDate.toString();
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate =  (createdDate == null) ? null : Instant.parse(createdDate);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getDisplayName(), getCreatedDate());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomerReference)) {
            return false;
        }
        CustomerReference that = (CustomerReference) o;
        return Objects.equals(getId(), that.getId())
               && Objects.equals(getDisplayName(), that.getDisplayName())
               && Objects.equals(getCreatedDate(), that.getCreatedDate());
    }
}
