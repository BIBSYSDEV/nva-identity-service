package no.unit.nva.customer.model.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.LinkedDataContextUtils;
import no.unit.nva.customer.model.interfaces.WithContext;
import nva.commons.core.JacocoGenerated;

import java.net.URI;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonTypeName("Customer")
public class CustomerResponse extends CustomerDto implements WithContext {

    @JsonProperty("@context")
    private URI context;

    public CustomerResponse() {
        super();
    }

    @Override
    public URI getContext() {
        return context;
    }

    @Override
    public void setContext(URI context) {
        this.context = context;
    }


    public static CustomerResponse toCustomerResponse(CustomerDao dao) {
        return toCustomerResponse(dao.toCustomerDto());
    }

    public static CustomerResponse toCustomerResponse(CustomerDto dto) {
        CustomerResponse customer = new CustomerResponse();

        customer.setCname(dto.getCname());
        customer.setName(dto.getName());
        customer.setIdentifier(dto.getIdentifier());
        customer.setArchiveName(dto.getArchiveName());
        customer.setCreatedDate(dto.getCreatedDate());
        customer.setDisplayName(dto.getDisplayName());
        customer.setInstitutionDns(dto.getInstitutionDns());
        customer.setShortName(dto.getShortName());
        customer.setVocabularies(dto.getVocabularies());
        customer.setModifiedDate(dto.getModifiedDate());
        customer.setFeideOrganizationId(dto.getFeideOrganizationId());
        customer.setCristinId(dto.getCristinId());
        customer.setLoginMethods(dto.getLoginMethods());

        customer.setContext(LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE);
        customer.setId(LinkedDataContextUtils.toId(dto.getIdentifier()));

        return customer;
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
        CustomerResponse that = (CustomerResponse) o;
        return Objects.equal(getId(), that.getId())
                && Objects.equal(getIdentifier(), that.getIdentifier())
                && Objects.equal(getCreatedDate(), that.getCreatedDate())
                && Objects.equal(getModifiedDate(), that.getModifiedDate())
                && Objects.equal(getName(), that.getName())
                && Objects.equal(getDisplayName(), that.getDisplayName())
                && Objects.equal(getShortName(), that.getShortName())
                && Objects.equal(getArchiveName(), that.getArchiveName())
                && Objects.equal(getCname(), that.getCname())
                && Objects.equal(getInstitutionDns(), that.getInstitutionDns())
                && Objects.equal(getFeideOrganizationId(), that.getFeideOrganizationId())
                && Objects.equal(getCristinId(), that.getCristinId())
                && Objects.equal(getVocabularies(), that.getVocabularies())
                && Objects.equal(getLoginMethods(), that.getLoginMethods())
                && Objects.equal(getContext(), that.getContext());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hashCode(getId(), getIdentifier(), getCreatedDate(), getModifiedDate(), getName(),
                getDisplayName(), getShortName(), getArchiveName(), getCname(), getInstitutionDns(),
                getFeideOrganizationId(), getCristinId(), getVocabularies(), getLoginMethods(), getContext());
    }
}
