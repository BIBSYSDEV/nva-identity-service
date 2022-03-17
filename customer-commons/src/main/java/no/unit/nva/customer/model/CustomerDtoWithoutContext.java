package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.interfaces.Resource;
import no.unit.nva.customer.model.interfaces.Typed;
import nva.commons.core.JacocoGenerated;

public class CustomerDtoWithoutContext implements Resource, Typed {

    private static final String TYPE = "Customer";
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
    private List<VocabularyDto> vocabularySettings;


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
        this.createdDate = parseInstant(createdDate);
    }

    public String getModifiedDate() {
        return nonNull(modifiedDate) ? modifiedDate.toString() : null;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = parseInstant(modifiedDate);
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

    public URI getCristinId() {
        return cristinId;
    }

    public void setCristinId(URI cristinId) {
        this.cristinId = cristinId;
    }

    public List<VocabularyDto> getVocabularies() {
        return nonNull(vocabularySettings) ? vocabularySettings : Collections.emptyList();
    }

    public void setVocabularies(List<VocabularyDto> vocabularySettings) {
        this.vocabularySettings = nonNull(vocabularySettings)
                                      ? vocabularySettings.stream().distinct().collect(Collectors.toList())
                                      : Collections.emptyList();
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getIdentifier(), getCreatedDate(), getModifiedDate(), getName(), getDisplayName(),
                            getShortName(), getArchiveName(), getCname(), getInstitutionDns(), getFeideOrganizationDomain(),
                            getCristinId(), getVocabularies());
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
               && Objects.equals(getFeideOrganizationDomain(), that.getFeideOrganizationDomain())
               && Objects.equals(getCristinId(), that.getCristinId())
               && Objects.equals(getVocabularies(), that.getVocabularies());
    }

    @Override
    public String getType() {
        return CustomerDtoWithoutContext.TYPE;
    }

    @Override
    public void setType(String type) {
        Typed.super.setType(type);
    }

    private Instant parseInstant(String createdDate) {
        return nonNull(createdDate) ? Instant.parse(createdDate) : null;
    }
}
