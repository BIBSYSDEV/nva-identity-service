package no.unit.nva.customer.model;

import com.google.common.base.Objects;
import no.unit.nva.customer.model.interfaces.WithDataFields;
import no.unit.nva.customer.model.interfaces.WithId;
import no.unit.nva.customer.model.interfaces.WithInternalFields;
import no.unit.nva.customer.model.interfaces.WithLoginMethods;
import no.unit.nva.customer.model.interfaces.WithVocabulary;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class CustomerDto
        implements WithId, WithDataFields, WithInternalFields, WithVocabulary<VocabularyDto>, WithLoginMethods {

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
    private Set<VocabularyDto> vocabularies;
    private LoginMethods loginMethods;

    public CustomerDto() {
        this.vocabularies = Collections.emptySet();
        this.loginMethods = new LoginMethods();
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
    public LoginMethods getLoginMethods() {
        return loginMethods;
    }

    @Override
    public void setLoginMethods(LoginMethods loginMethods) {
        this.loginMethods = loginMethods;
    }

    @Override
    public Set<VocabularyDto> getVocabularies() {
        return vocabularies;
    }

    @Override
    public void setVocabularies(Set<VocabularyDto> vocabularies) {
        this.vocabularies = vocabularies;
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
                && Objects.equal(getLoginMethods(), that.getLoginMethods());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hashCode(getId(), getIdentifier(), getCreatedDate(), getModifiedDate(), getName(),
                getDisplayName(), getShortName(), getArchiveName(), getCname(), getInstitutionDns(),
                getFeideOrganizationId(), getCristinId(), getVocabularies(), getLoginMethods());
    }
}
