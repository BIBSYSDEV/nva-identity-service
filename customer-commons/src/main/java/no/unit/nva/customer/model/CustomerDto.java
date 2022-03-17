package no.unit.nva.customer.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import no.unit.nva.customer.model.interfaces.Context;
import no.unit.nva.customer.model.interfaces.Typed;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

//Overriding setters and getters is necessary for Jackson-Jr
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.UselessOverridingMethod"})
public class CustomerDto extends CustomerDtoWithoutContext implements Context {

    public static final String TYPE = "Customer";
    @JsonProperty("@context")
    private URI context;

    public CustomerDto() {
        super();
        setVocabularies(Collections.emptyList());
    }

    public static CustomerDto fromJson(String json) {
        return attempt(() -> JsonConfig.beanFrom(CustomerDto.class, json))
            .orElseThrow(fail -> new BadRequestException("Could not parse input:" + json,fail.getException()));
    }

    public static Builder builder() {
        return new Builder();
    }

    public CustomerDtoWithoutContext withoutContext() {
        return attempt(() -> JsonConfig.asString(this))
            .map(json -> JsonConfig.beanFrom(CustomerDtoWithoutContext.class, json))
            .orElseThrow();
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
            .withVocabularies(getVocabularies())
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
                            getCristinId(), getVocabularies(), getContext());
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
               && Objects.equals(getVocabularies(), that.getVocabularies())
               && Objects.equals(getContext(), that.getContext());
    }

    @Override
    @JsonProperty(Typed.TYPE_FIELD)
    public String getType() {
        return TYPE;
    }

    @Override
    public void setType(String type) {
        super.setType(type);
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return attempt(() -> JsonConfig.asString(this)).orElseThrow();
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

        public Builder withCreatedDate(String createdDate) {
            customerDto.setCreatedDate(createdDate);
            return this;
        }

        public Builder withModifiedDate(String modifiedDate) {
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

        public Builder withVocabularies(Collection<VocabularyDto> vocabularies) {
            customerDto.setVocabularies(new ArrayList<>(vocabularies));
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
