package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.customer.model.interfaces.Context;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

@JsonTypeName("Customer")
public class CustomerDtoWithContext extends CustomerDto implements Context {

    @JsonProperty("@context")
    private URI context;

    public CustomerDtoWithContext() {
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

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerDtoWithContext that = (CustomerDtoWithContext) o;
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getIdentifier(), that.getIdentifier())
                && Objects.equals(getCreatedDate(), that.getCreatedDate())
                && Objects.equals(getModifiedDate(), that.getModifiedDate())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getDisplayName(), that.getDisplayName())
                && Objects.equals(getShortName(), that.getShortName())
                && Objects.equals(getArchiveName(), that.getArchiveName())
                && Objects.equals(getCname(), that.getCname())
                && Objects.equals(getInstitutionDns(), that.getInstitutionDns())
                && Objects.equals(getFeideOrganizationId(), that.getFeideOrganizationId())
                && Objects.equals(getCristinId(), that.getCristinId())
                && Objects.equals(getVocabularySettings(), that.getVocabularySettings())
                && Objects.equals(getContext(), that.getContext());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getIdentifier(), getCreatedDate(), getModifiedDate(), getName(), getDisplayName(),
                getShortName(), getArchiveName(), getCname(), getInstitutionDns(), getFeideOrganizationId(),
                getCristinId(), getVocabularySettings(), getContext());
    }
}
