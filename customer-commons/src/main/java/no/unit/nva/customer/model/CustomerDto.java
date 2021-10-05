package no.unit.nva.customer.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.model.interfaces.Context;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;

public class CustomerDto extends CustomerDtoWithoutContext implements Context {

    @JsonProperty("@context")
    private URI context;

    public CustomerDto() {
        super();
        setVocabularySettings(Collections.emptySet());
    }

    public CustomerDtoWithoutContext withoutContext() {
        return attempt(() -> JsonUtils.objectMapper.convertValue(this, CustomerDtoWithoutContext.class)).orElseThrow();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public URI getContext() {
        return context;
    }

    @Override
    public void setContext(URI context) {
        this.context = context;
    }

    public Builder copy() {
        return new Builder()
            .withVocabularySettings(getVocabularySettings())
            .withShortName(getShortName())
            .withInstitutionDns(getInstitutionDns())
            .withDisplayName(getDisplayName())
            .withCreatedDate(getCreatedDate())
            .withArchiveName(getArchiveName())
            .withIdentifier(getIdentifier())
            .withContext(getContext())
            .withCname(getCname())
            .withId(getId())
            .withCristinId(getCristinId())
            .withFeideOrganizationId(getFeideOrganizationId())
            .withName(getName())
            .withModifiedDate(getModifiedDate());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getIdentifier(), getCreatedDate(), getModifiedDate(), getName(), getDisplayName(),
                            getShortName(), getArchiveName(), getCname(), getInstitutionDns(), getFeideOrganizationId(),
                            getCristinId(), getVocabularySettings(), getContext());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomerDto)) {
            return false;
        }
        CustomerDto that = (CustomerDto) o;
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

    public static final class Builder {

        private final CustomerDto customerDto;

        private Builder() {
            customerDto = new CustomerDto();
        }

        public Builder withId(URI id) {
            customerDto.setId(id);
            return this;
        }

        public Builder withIdentifier(UUID identifier) {
            customerDto.setIdentifier(identifier);
            return this;
        }

        public Builder withCreatedDate(Instant createdDate) {
            customerDto.setCreatedDate(createdDate);
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            customerDto.setModifiedDate(modifiedDate);
            return this;
        }

        public Builder withName(String name) {
            customerDto.setName(name);
            return this;
        }

        public Builder withDisplayName(String displayName) {
            customerDto.setDisplayName(displayName);
            return this;
        }

        public Builder withShortName(String shortName) {
            customerDto.setShortName(shortName);
            return this;
        }

        public Builder withArchiveName(String archiveName) {
            customerDto.setArchiveName(archiveName);
            return this;
        }

        public Builder withCname(String cname) {
            customerDto.setCname(cname);
            return this;
        }

        public Builder withInstitutionDns(String institutionDns) {
            customerDto.setInstitutionDns(institutionDns);
            return this;
        }

        public Builder withFeideOrganizationId(String feideOrganizationId) {
            customerDto.setFeideOrganizationId(feideOrganizationId);
            return this;
        }

        public Builder withCristinId(String cristinId) {
            customerDto.setCristinId(cristinId);
            return this;
        }

        public Builder withVocabularySettings(Set<VocabularySettingDto> vocabularySettings) {
            customerDto.setVocabularySettings(vocabularySettings);
            return this;
        }

        public Builder withContext(URI context) {
            customerDto.setContext(context);
            return this;
        }

        public CustomerDto build() {
            return customerDto;
        }
    }
}
