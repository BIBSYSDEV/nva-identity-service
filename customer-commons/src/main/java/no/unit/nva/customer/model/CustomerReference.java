package no.unit.nva.customer.model;

import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class CustomerReference {

    private URI id;
    private String displayName;
    private Instant createdDate;

    private String doiPrefix;

    public static CustomerReference fromCustomerDto(CustomerDto customerDto) {
        var customerReference = new CustomerReference();
        customerReference.setDisplayName(customerDto.getDisplayName());
        customerReference.setId(customerDto.getId());
        customerReference.setCreatedDate(customerDto.getCreatedDate());
        customerReference.setDoiPrefix(extractDoiPrefix(customerDto));
        return customerReference;
    }

    private static String extractDoiPrefix(CustomerDto customerDto) {
        return Optional
                .ofNullable(customerDto.getDoiAgent())
                .map(CustomerDto.DoiAgentDto::getPrefix)
                .orElse(null);
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

    public String getDoiPrefix() {
        return doiPrefix;
    }

    public void setDoiPrefix(String doiPrefix) {
        this.doiPrefix = doiPrefix;
    }

    @SuppressWarnings({"PMD.NullAssignment"})
    public String getCreatedDate() {
        return nonNull(createdDate) ? createdDate.toString() : null;
    }

    @SuppressWarnings({"PMD.NullAssignment"})
    public void setCreatedDate(String createdDate) {
        this.createdDate = nonNull(createdDate) ? Instant.parse(createdDate) : null;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(),
                getDisplayName(),
                getDoiPrefix(),
                getCreatedDate());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomerReference that)) {
            return false;
        }
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getDisplayName(), that.getDisplayName())
                && Objects.equals(getDoiPrefix(), that.getDoiPrefix())
                && Objects.equals(getCreatedDate(), that.getCreatedDate());
    }
}
