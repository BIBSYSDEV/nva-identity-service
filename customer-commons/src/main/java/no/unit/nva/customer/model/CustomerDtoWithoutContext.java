package no.unit.nva.customer.model;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.model.interfaces.Customer;
import no.unit.nva.customer.model.interfaces.Resource;
import nva.commons.core.JacocoGenerated;


public class CustomerDtoWithoutContext implements Customer<VocabularyDto>, Resource {

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
    private String feideOrganizationId;
    private String cristinId;
    private Set<VocabularyDto> vocabularySettings;

    public CustomerDtoWithoutContext() {

    }

    @Override
    public URI getId() {
        return id;
    }

    @Override
    public void setId(URI id) {
        this.id = id;
    }

    @Override
    public UUID getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(UUID identifier) {
        this.identifier = identifier;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getShortName() {
        return shortName;
    }

    @Override
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String getArchiveName() {
        return archiveName;
    }

    @Override
    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    @Override
    public String getCname() {
        return cname;
    }

    @Override
    public void setCname(String cname) {
        this.cname = cname;
    }

    @Override
    public String getInstitutionDns() {
        return institutionDns;
    }

    @Override
    public void setInstitutionDns(String institutionDns) {
        this.institutionDns = institutionDns;
    }

    @Override
    public String getFeideOrganizationId() {
        return feideOrganizationId;
    }

    @Override
    public void setFeideOrganizationId(String feideOrganizationId) {
        this.feideOrganizationId = feideOrganizationId;
    }

    @Override
    public String getCristinId() {
        return cristinId;
    }

    @Override
    public void setCristinId(String cristinId) {
        this.cristinId = cristinId;
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
        CustomerDtoWithoutContext that = (CustomerDtoWithoutContext) o;
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
               && Objects.equals(getVocabularies(), that.getVocabularies());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getIdentifier(), getCreatedDate(), getModifiedDate(), getName(), getDisplayName(),
                            getShortName(), getArchiveName(), getCname(), getInstitutionDns(), getFeideOrganizationId(),
                            getCristinId(), getVocabularies());
    }

    @Override
    public Set<VocabularyDto> getVocabularies() {
        return vocabularySettings;
    }

    @Override
    public void setVocabularies(Set<VocabularyDto> vocabularySettings) {
        this.vocabularySettings = vocabularySettings;
    }
}