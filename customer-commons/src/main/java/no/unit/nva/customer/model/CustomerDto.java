package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.customer.model.LinkedDataContextUtils.toId;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        this.createdDate = (createdDate == null) ? null : Instant.parse(createdDate);
    }

    public String getModifiedDate() {
        return (modifiedDate == null) ? null : modifiedDate.toString();
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = (modifiedDate == null) ? null : Instant.parse(modifiedDate);
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
                   .withCreatedDate(getCreatedDate())
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
                   .withModifiedDate(getModifiedDate())
                   .withRorId(getRorId())
                   .withPublicationWorkflow(getPublicationWorkflow());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getContext(), getId(), getIdentifier(), getCreatedDate(), getModifiedDate(), getName(),
                            getDisplayName(), getShortName(), getArchiveName(), getCname(), getInstitutionDns(),
                            getFeideOrganizationDomain(), getCristinId(), getCustomerOf(), getVocabularies(),
                            getRorId(), getPublicationWorkflow());
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
        return Objects.equals(getArchiveName(), that.getArchiveName())
               && Objects.equals(getContext(), that.getContext())
               && Objects.equals(getCname(), that.getCname())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getCristinId(), that.getCristinId())
               && Objects.equals(getCustomerOf(), that.getCustomerOf())
               && Objects.equals(getDisplayName(), that.getDisplayName())
               && Objects.equals(getFeideOrganizationDomain(), that.getFeideOrganizationDomain())
               && Objects.equals(getId(), that.getId())
               && Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getInstitutionDns(), that.getInstitutionDns())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getName(), that.getName())
               && Objects.equals(getRorId(), that.getRorId())
               && Objects.equals(getShortName(), that.getShortName())
               && Objects.equals(getVocabularies(), that.getVocabularies())
               && getPublicationWorkflow() == that.getPublicationWorkflow();
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
            customerDto.createdDate = createdDate;
            return this;
        }

        public Builder withCreatedDate(String createdDate) {
            customerDto.setCreatedDate(createdDate);
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            customerDto.modifiedDate = modifiedDate;
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
            customerDto.setDoiAgent(
                doiAgent == null
                ? null
                : new DoiAgentDto(doiAgent));
            var agent = doiAgent == null
                            ? null
                            : new DoiAgentDto(doiAgent);
            var urlId = (customerDto.identifier == null)
                                    ?  URI.create("https://example.org/cutommer/xxx")
                                    : toId(customerDto.identifier);

            agent.addLink("self",urlId + "/doiAgent")
                 .addLink("doi",urlId + "/doiAgent/doi")
                 .addLink("secret",urlId + "/doiAgent/secret");

            customerDto.setDoiAgent(agent);

            return this;
        }

        public CustomerDto build() {
            return customerDto;
        }
    }

    public static class DoiAgentDto implements DoiAgent {

        private String prefix;
        private String name;
        private final Map<String, LinkItem> links = new HashMap<>(3);

        public DoiAgentDto() {
        }

        public DoiAgentDto(DoiAgent doiAgent) {
            this.prefix = doiAgent.getPrefix();
            this.name = doiAgent.getName();
                this.addLink("self","https://example.org/124323453120581/doiagent")
                    .addLink("fetchDoi","https://example.org/124323453120581/doiagent/doi");
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getName() {
            return name;
        }

        public Map<String, LinkItem> getLinks() {
            return links;
        }

        public DoiAgentDto addLink(String name, String url) {
            try {
                if (!links.containsKey(name)) {
                    links.put(name, LinkItem.newLink(url));
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            return this;
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
            return Objects.equals(getPrefix(), that.getPrefix())
                   && Objects.equals(getName(), that.getName())
                   && Objects.equals(getLinks(), that.getLinks());
        }

        @Override
        @JacocoGenerated
        public int hashCode() {
            return Objects.hash(getPrefix(), getName(), getLinks());
        }

        @Override
        @JacocoGenerated
        public String toString() {
            return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
        }
    }
}