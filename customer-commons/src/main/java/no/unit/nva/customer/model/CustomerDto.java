package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.customer.model.LinkedDataContextUtils.toId;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import no.unit.nva.customer.model.interfaces.Context;
import no.unit.nva.customer.model.interfaces.DoiAgent;
import no.unit.nva.customer.model.interfaces.RightsRetentionStrategy;
import no.unit.nva.customer.model.interfaces.Typed;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

//Overriding setters and getters is necessary for Jackson-Jr
@SuppressWarnings({"PMD.ExcessivePublicCount",
    "PMD.UselessOverridingMethod",
    "PMD.TooManyFields",
    "PMD.GodClass",
    "PMD.NullAssignment"})
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
    private URI serviceCenterUri;
    private PublicationWorkflow publicationWorkflow;
    private DoiAgentDto doiAgent;
    private boolean nviInstitution;
    private boolean rboInstitution;
    private Instant inactiveFrom;
    private Sector sector;
    private RightsRetentionStrategyDto rightsRetentionStrategy;
    private Set<PublicationInstanceTypes> allowFileUploadForTypes;

    public CustomerDto() {
        super();
        this.vocabularies = Collections.emptyList();
        this.allowFileUploadForTypes = Collections.emptySet();
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
        return nonNull(createdDate) ? createdDate.toString() : null;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = nonNull(createdDate) ? Instant.parse(createdDate) : null;
    }

    public String getModifiedDate() {
        return nonNull(modifiedDate) ? modifiedDate.toString() : null;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = nonNull(modifiedDate) ? Instant.parse(modifiedDate) : null;
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

    public void setDoiAgent(DoiAgentDto doiAgent) {
        this.doiAgent = doiAgent;
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

    public URI getServiceCenterUri() {
        return serviceCenterUri;
    }

    public void setServiceCenterUri(URI serviceCenterUri) {
        this.serviceCenterUri = serviceCenterUri;
    }

    public PublicationWorkflow getPublicationWorkflow() {
        return publicationWorkflow;
    }

    public void setPublicationWorkflow(PublicationWorkflow publicationWorkflow) {
        this.publicationWorkflow = publicationWorkflow;
    }

    public boolean isNviInstitution() {
        return nviInstitution;
    }

    public void setNviInstitution(boolean nviInstitution) {
        this.nviInstitution = nviInstitution;
    }

    public boolean isRboInstitution() {
        return rboInstitution;
    }

    public void setRboInstitution(boolean rboInstitution) {
        this.rboInstitution = rboInstitution;
    }

    public Instant getInactiveFrom() {
        return inactiveFrom;
    }

    public void setInactiveFrom(Instant inactiveFrom) {
        this.inactiveFrom = inactiveFrom;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public Set<PublicationInstanceTypes> getAllowFileUploadForTypes() {
        if (allowFileUploadForTypes == null || allowFileUploadForTypes.isEmpty()) {
            return defaultAllowFileUploadForTypes();
        }
        return allowFileUploadForTypes;
    }

    @JsonIgnore
    public boolean isActive() {
        return !nonNull(inactiveFrom) || isInactiveFromInFuture();

    }

    @JsonIgnore
    private boolean isInactiveFromInFuture() {
        var now = Instant.now();
        return now.isBefore(inactiveFrom);
    }

    private Set<PublicationInstanceTypes> defaultAllowFileUploadForTypes() {
        return Set.of(PublicationInstanceTypes.values());
    }

    public void setAllowFileUploadForTypes(Set<PublicationInstanceTypes> allowFileUploadForTypes) {
        this.allowFileUploadForTypes = allowFileUploadForTypes;
    }

    public RightsRetentionStrategyDto getRightsRetentionStrategy() {
        return rightsRetentionStrategy;
    }

    public void setRightsRetentionStrategy(RightsRetentionStrategyDto rightsRetentionStrategy) {
        this.rightsRetentionStrategy = rightsRetentionStrategy;
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
                .withArchiveName(getArchiveName())
                .withCname(getCname())
                .withContext(getContext())
                .withCreatedDate(getCreatedDate())
                .withCristinId(getCristinId())
                .withCustomerOf(getCustomerOf())
                .withDisplayName(getDisplayName())
                .withFeideOrganizationDomain(getFeideOrganizationDomain())
                .withId(getId())
                .withIdentifier(getIdentifier())
                .withInstitutionDns(getInstitutionDns())
                .withModifiedDate(getModifiedDate())
                .withName(getName())
                .withPublicationWorkflow(getPublicationWorkflow())
                .withRorId(getRorId())
                .withServiceCenterUri(getServiceCenterUri())
                .withShortName(getShortName())
                .withDoiAgent(getDoiAgent())
                .withNviInstitution(isNviInstitution())
                .withRboInstitution(isRboInstitution())
                .withInactiveFrom(getInactiveFrom())
                .withSector(getSector())
                .withVocabularies(getVocabularies())
                .withRightsRetentionStrategy(getRightsRetentionStrategy())
                .withAllowFileUploadForTypes(getAllowFileUploadForTypes());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getContext(), getId(), getIdentifier(), getCreatedDate(), getModifiedDate(), getName(),
                            getDisplayName(), getShortName(), getArchiveName(), getCname(), getInstitutionDns(),
                            getFeideOrganizationDomain(), getCristinId(), getCustomerOf(), getVocabularies(),
                            getRorId(), getServiceCenterUri(), getPublicationWorkflow(), getDoiAgent(),
                            getRightsRetentionStrategy(), getAllowFileUploadForTypes(), getInactiveFrom());
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
                && Objects.equals(getInactiveFrom(), that.getInactiveFrom())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getRorId(), that.getRorId())
                && Objects.equals(getServiceCenterUri(), that.getServiceCenterUri())
                && Objects.equals(getShortName(), that.getShortName())
                && Objects.equals(getVocabularies(), that.getVocabularies())
                && Objects.equals(getDoiAgent(), that.getDoiAgent())
                && Objects.equals(getRightsRetentionStrategy(), that.getRightsRetentionStrategy())
                && getPublicationWorkflow() == that.getPublicationWorkflow()
                && Objects.equals(getAllowFileUploadForTypes(), that.getAllowFileUploadForTypes());
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

        public Builder withServiceCenterUri(URI servceCenterUri) {
            customerDto.setServiceCenterUri(servceCenterUri);
            return this;
        }

        public Builder withPublicationWorkflow(PublicationWorkflow publicationWorkflow) {
            customerDto.setPublicationWorkflow(publicationWorkflow);
            return this;
        }

        public Builder withDoiAgent(DoiAgent doiAgent) {
            customerDto.setDoiAgent(buildDoiAgentDto(doiAgent));
            return this;
        }

        public Builder withNviInstitution(boolean hasNviInstitution) {
            customerDto.setNviInstitution(hasNviInstitution);
            return this;
        }

        public Builder withRboInstitution(boolean rboInstitution) {
            customerDto.setRboInstitution(rboInstitution);
            return this;
        }

        public Builder withInactiveFrom(Instant inactiveFrom) {
            customerDto.setInactiveFrom(inactiveFrom);
            return this;
        }

        public Builder withSector(Sector sector) {
            customerDto.setSector(sector);
            return this;
        }

        public Builder withAllowFileUploadForTypes(Set<PublicationInstanceTypes> allowFileUploadForTypes) {
            customerDto.setAllowFileUploadForTypes(allowFileUploadForTypes);
            return this;
        }

        public Builder withRightsRetentionStrategy(RightsRetentionStrategy rightsRetentionStrategy) {
            customerDto.setRightsRetentionStrategy(buildRetentionStrategyDto(rightsRetentionStrategy));
            return this;
        }

        private DoiAgentDto buildDoiAgentDto(DoiAgent doiAgent) {
            if (nonNull(doiAgent)) {
                return nonNull(customerDto.identifier)
                        ? new DoiAgentDto(doiAgent).addIdByIdentifier(customerDto.identifier)
                        : new DoiAgentDto(doiAgent);
            }
            return null;
        }

        private RightsRetentionStrategyDto buildRetentionStrategyDto(RightsRetentionStrategy rightsRetentionStrategy) {
            if (nonNull(rightsRetentionStrategy)) {
                return new RightsRetentionStrategyDto(rightsRetentionStrategy);
            }
            return null;
        }

        public CustomerDto build() {
            return customerDto;
        }
    }

    public static class DoiAgentDto implements DoiAgent {

        private URI id;
        private String url;
        private String prefix;
        private String username;
        private String password;

        @SuppressWarnings("unused")
        public DoiAgentDto() {
        }

        public DoiAgentDto(DoiAgent doiAgent) {
            this.username = doiAgent.getUsername();
            this.url = doiAgent.getUrl();
            this.prefix = doiAgent.getPrefix();
        }

        public static DoiAgentDto fromJson(String json) throws BadRequestException {
            return attempt(() -> JsonConfig.readValue(json, DoiAgentDto.class)).orElseThrow(
                    fail -> new BadRequestException("Could not parse input:" + json, fail.getException()));
        }

        public URI getId() {
            return id;
        }

        public void setId(URI id) {
            this.id = id;
        }

        public DoiAgentDto addId(URI doiAgentId) {
            this.id = doiAgentId;
            return this;
        }

        /**
         * Takes a customer identifier and build a doiAgent id.
         */
        public DoiAgentDto addIdByIdentifier(UUID identifier) {
            this.id = UriWrapper
                    .fromUri(toId(identifier))
                    .addChild(DOI_AGENT)
                    .getUri();
            return this;
        }

        @Override
        public String getUsername() {
            return username;
        }

        public void setUsername(String name) {
            this.username = name;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public DoiAgentDto addPassword(String secretString) {
            password = secretString;
            return this;
        }

        @Override
        @JacocoGenerated
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DoiAgentDto that)) {
                return false;
            }
            return Objects.equals(getId(), that.getId())
                    && Objects.equals(getUrl(), that.getUrl())
                    && Objects.equals(getPrefix(), that.getPrefix())
                    && Objects.equals(getUsername(), that.getUsername())
                    && Objects.equals(getPassword(), that.getPassword());
        }

        @Override
        @JacocoGenerated
        public int hashCode() {
            return Objects.hash(getId(), getUrl(), getPrefix(), getUsername(), getPassword());
        }

        @Override
        @JacocoGenerated
        public String toString() {
            return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
        }
    }
}