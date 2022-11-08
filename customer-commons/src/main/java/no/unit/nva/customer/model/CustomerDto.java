package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import no.unit.nva.customer.model.interfaces.Context;
import no.unit.nva.customer.model.interfaces.DoiAgent;
import no.unit.nva.customer.model.interfaces.Typed;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

//Overriding setters and getters is necessary for Jackson-Jr
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.UselessOverridingMethod", "PMD.TooManyFields", "PMD.GodClass"})
public class CustomerDto implements Context {

    public static final String TYPE = "Customer";
    @JsonProperty("@context")
    private URI context;
    private URI id;
    private UUID identifier;
    private Instant createdDate;
    private Instant modifiedDate;
    private String name;
    private String displayName;
    private String shortName;
    private String archiveName;
    private String cname;
    private String institutionDns;
    private String feideOrganizationDomain;
    private URI cristinId;
    private ApplicationDomain customerOf;
    private List<VocabularyDto> vocabularies;
    private URI rorId;
    private PublicationWorkflow publicationWorkflow;
    private DoiAgentDto doiAgent;

    public CustomerDto() {
        super();
        this.vocabularies = Collections.emptyList();
    }

    public static CustomerDto fromJson(String json) throws BadRequestException {
        return attempt(() -> JsonConfig.readValue(json, CustomerDto.class)).orElseThrow(
            fail -> new BadRequestException("Could not parse input:" + json, fail.getException()));
    }

    public static Builder builder() {
        return new Builder();
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public UUID getIdentifier() {
        return identifier;
    }

    public void setIdentifier(UUID identifier) {
        this.identifier = identifier;
    }

    public String getCreatedDate() {
        return (createdDate == null) ? null : createdDate.toString();
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = Instant.parse(createdDate);
    }

    public String getModifiedDate() {
        return (modifiedDate == null) ? null : modifiedDate.toString();
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = Instant.parse(modifiedDate);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public String getInstitutionDns() {
        return institutionDns;
    }

    public void setInstitutionDns(String institutionDns) {
        this.institutionDns = institutionDns;
    }

    public String getFeideOrganizationDomain() {
        return feideOrganizationDomain;
    }

    public void setFeideOrganizationDomain(String feideOrganizationDomain) {
        this.feideOrganizationDomain = feideOrganizationDomain;
    }

    public DoiAgentDto getDoiAgent() {
        return doiAgent;
    }

    public void setDoiAgent(DoiAgentDto doi) {
        this.doiAgent = doi;
    }

    public URI getCristinId() {
        return cristinId;
    }

    public void setCristinId(URI cristinId) {
        this.cristinId = cristinId;
    }

    public ApplicationDomain getCustomerOf() {
        return customerOf;
    }

    public void setCustomerOf(ApplicationDomain customerOf) {
        this.customerOf = customerOf;
    }

    public List<VocabularyDto> getVocabularies() {
        return vocabularies;
    }

    public void setVocabularies(List<VocabularyDto> vocabularies) {
        this.vocabularies = vocabularies;
    }

    public URI getRorId() {
        return rorId;
    }

    public void setRorId(URI rorId) {
        this.rorId = rorId;
    }

    public PublicationWorkflow getPublicationWorkflow() {
        return publicationWorkflow;
    }

    public void setPublicationWorkflow(PublicationWorkflow publicationWorkflow) {
        this.publicationWorkflow = publicationWorkflow;
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
        return new Builder().withVocabularies(getVocabularies())
                   .withShortName(getShortName())
                   .withInstitutionDns(getInstitutionDns())
                   .withDisplayName(getDisplayName())
                   .withCreatedDate(Instant.parse(getCreatedDate()))
                   .withArchiveName(getArchiveName())
                   .withIdentifier(getIdentifier())
                   .withContext(getContext())
                   .withCname(getCname())
                   .withId(getId())
                   .withCristinId(getCristinId())
                   .withCustomerOf(getCustomerOf())
                   .withFeideOrganizationDomain(getFeideOrganizationDomain())
                   .withDoiAgent(getDoiAgent())
                   .withName(getName())
                   .withModifiedDate(Instant.parse(getModifiedDate()))
                   .withRorId(getRorId())
                   .withPublicationWorkflow(getPublicationWorkflow());
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
        CustomerDto that = (CustomerDto) o;
        return Objects.equals(context, that.context)
               && Objects.equals(id, that.id)
               && Objects.equals(identifier, that.identifier)
               && Objects.equals(createdDate, that.createdDate)
               && Objects.equals(modifiedDate, that.modifiedDate)
               && Objects.equals(name, that.name)
               && Objects.equals(displayName, that.displayName)
               && Objects.equals(shortName, that.shortName)
               && Objects.equals(archiveName, that.archiveName)
               && Objects.equals(cname, that.cname)
               && Objects.equals(institutionDns, that.institutionDns)
               && Objects.equals(feideOrganizationDomain, that.feideOrganizationDomain)
               && Objects.equals(cristinId, that.cristinId)
               && customerOf == that.customerOf
               && Objects.equals(vocabularies, that.vocabularies)
               && Objects.equals(rorId, that.rorId)
               && publicationWorkflow == that.publicationWorkflow
               && Objects.equals(doiAgent, that.doiAgent);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(context, id, identifier, createdDate, modifiedDate, name, displayName, shortName,
                            archiveName,
                            cname, institutionDns, feideOrganizationDomain, cristinId, customerOf, vocabularies, rorId,
                            publicationWorkflow, doiAgent);
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }

    @JsonProperty(Typed.TYPE_FIELD)
    public String getType() {
        return TYPE;
    }

    public void setType(String type) {
        // do nothing;
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
            customerDto.setCreatedDate(createdDate.toString());
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            customerDto.setModifiedDate(modifiedDate.toString());
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

        public Builder withFeideOrganizationDomain(String feideOrganizationDomain) {
            customerDto.setFeideOrganizationDomain(feideOrganizationDomain);
            return this;
        }

        public Builder withCristinId(URI cristinId) {
            customerDto.setCristinId(cristinId);
            return this;
        }

        public Builder withCustomerOf(ApplicationDomain customerOf) {
            customerDto.setCustomerOf(customerOf);
            return this;
        }

        public Builder withVocabularies(Collection<VocabularyDto> vocabularies) {
            if (nonNull(vocabularies)) {
                customerDto.setVocabularies(new ArrayList<>(vocabularies));
            }
            return this;
        }

        public Builder withContext(URI context) {
            customerDto.setContext(context);
            return this;
        }

        public Builder withRorId(URI rorId) {
            customerDto.setRorId(rorId);
            return this;
        }

        public Builder withPublicationWorkflow(PublicationWorkflow publicationWorkflow) {
            customerDto.setPublicationWorkflow(publicationWorkflow);
            return this;
        }

        public Builder withDoiAgent(DoiAgent doiAgent) {
            customerDto.setDoiAgent(doiAgent != null ? new DoiAgentDto(doiAgent) : null);
            return this;
        }

        public CustomerDto build() {
            return customerDto;
        }
    }

    public static class DoiAgentDto implements DoiAgent {

        private String prefix;
        private String name;

        public DoiAgentDto() {
        }

        public DoiAgentDto(DoiAgent doiAgent) {
            this.prefix = doiAgent.getPrefix();
            this.name = doiAgent.getName();
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getName() {
            return name;
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
            DoiAgentDto that = (DoiAgentDto) o;
            return Objects.equals(prefix, that.prefix) && Objects.equals(name, that.name);
        }

        @Override
        @JacocoGenerated
        public int hashCode() {
            return Objects.hash(prefix, name);
        }

        @Override
        @JacocoGenerated
        public String toString() {
            return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
        }
    }
}

