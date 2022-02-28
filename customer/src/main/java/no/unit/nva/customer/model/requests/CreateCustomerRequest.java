package no.unit.nva.customer.model.requests;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.LoginMethods;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.interfaces.WithDataFields;
import no.unit.nva.customer.model.interfaces.WithLoginMethods;
import no.unit.nva.customer.model.interfaces.WithVocabulary;
import nva.commons.core.JacocoGenerated;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonTypeName("Customer")
public class CreateCustomerRequest implements WithDataFields, WithVocabulary<VocabularyDto>, WithLoginMethods {

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

    public CreateCustomerRequest() {
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

    public CustomerDao toCustomerDao() {
        Instant now = Instant.now();
        CustomerDao customer = new CustomerDao();

        customer.setArchiveName(getArchiveName());
        customer.setCname(getCname());
        customer.setCreatedDate(now);
        customer.setCristinId(getCristinId());
        customer.setDisplayName(getDisplayName());
        customer.setIdentifier(UUID.randomUUID());
        customer.setInstitutionDns(getInstitutionDns());
        customer.setShortName(getShortName());
        customer.setFeideOrganizationId(getFeideOrganizationId());
        customer.setModifiedDate(now);
        customer.setVocabularies(CustomerDao.extractVocabularySettings(this));
        customer.setName(getName());
        customer.setLoginMethods(getLoginMethods());

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
        CreateCustomerRequest that = (CreateCustomerRequest) o;
        return Objects.equal(getName(), that.getName())
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
        return Objects.hashCode(getName(), getDisplayName(), getShortName(), getArchiveName(), getCname(),
                getInstitutionDns(), getFeideOrganizationId(), getCristinId(), getVocabularies(), getLoginMethods());
    }
}
