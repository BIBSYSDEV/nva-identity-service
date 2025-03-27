package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;

public class CustomerReference {

    private URI id;
    private URI cristinId;
    private String displayName;
    private Instant createdDate;
    private boolean active;
    private String doiPrefix;
    private boolean nviInstitution;

    public static CustomerReference fromCustomerDto(CustomerDto customerDto) {
        var customerReference = new CustomerReference();
        customerReference.setDisplayName(customerDto.getDisplayName());
        customerReference.setId(customerDto.getId());
        customerReference.setCristinId(customerDto.getCristinId());
        customerReference.setCreatedDate(customerDto.getCreatedDate());
        customerReference.setDoiPrefix(extractDoiPrefix(customerDto));
        customerReference.setActive(customerDto.isActive());
        customerReference.setNviInstitution(customerDto.isNviInstitution());
        return customerReference;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCristinId(), getDisplayName(), getCreatedDate(), isActive(), getDoiPrefix());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CustomerReference that)) {
            return false;
        }
        return isActive() == that.isActive()
               && Objects.equals(getId(), that.getId())
               && Objects.equals(getCristinId(), that.getCristinId())
               && Objects.equals(getDisplayName(), that.getDisplayName())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getDoiPrefix(), that.getDoiPrefix());
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public URI getCristinId() {
        return cristinId;
    }

    public void setCristinId(URI cristinId) {
        this.cristinId = cristinId;
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

    public boolean isNviInstitution() {
        return nviInstitution;
    }

    public void setNviInstitution(boolean nviInstitution) {
        this.nviInstitution = nviInstitution;
    }

    private static String extractDoiPrefix(CustomerDto customerDto) {
        return Optional.ofNullable(customerDto.getDoiAgent()).map(CustomerDto.DoiAgentDto::getPrefix).orElse(null);
    }
}
