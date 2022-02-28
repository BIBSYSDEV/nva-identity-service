package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import no.unit.nva.customer.model.interfaces.WithDataFields;
import no.unit.nva.customer.model.interfaces.WithInternalFields;
import no.unit.nva.customer.model.interfaces.WithLoginMethods;
import no.unit.nva.customer.model.interfaces.WithVocabulary;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(
    use = Id.NAME,
    include = As.PROPERTY,
    property = "type",
    defaultImpl = CustomerDao.class
)
@JsonTypeName("Customer")
public class CustomerDao
        implements WithDataFields, WithInternalFields, WithVocabulary<VocabularyDao>, WithLoginMethods {

    public static final String IDENTIFIER = "identifier";
    public static final String ORG_NUMBER = "feideOrganizationId";
    public static final String CRISTIN_ID = "cristinId";

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
    private Set<VocabularyDao> vocabularies;
    private LoginMethods loginMethods;

    public CustomerDao() {
        vocabularies = Collections.emptySet();
        loginMethods = new LoginMethods();
    }

    public static CustomerDao fromCustomerDto(CustomerDto dto) {
        CustomerDao customer = new CustomerDao();

        customer.setArchiveName(dto.getArchiveName());
        customer.setCname(dto.getCname());
        customer.setCreatedDate(dto.getCreatedDate());
        customer.setCristinId(dto.getCristinId());
        customer.setDisplayName(dto.getDisplayName());
        customer.setIdentifier(dto.getIdentifier());
        customer.setInstitutionDns(dto.getInstitutionDns());
        customer.setShortName(dto.getShortName());
        customer.setFeideOrganizationId(dto.getFeideOrganizationId());
        customer.setModifiedDate(dto.getModifiedDate());
        customer.setVocabularies(extractVocabularySettings(dto));
        customer.setName(dto.getName());
        customer.setLoginMethods(dto.getLoginMethods());

        return customer;
    }

    public CustomerDao copy() {
        CustomerDao customer = new CustomerDao();

        customer.setArchiveName(getArchiveName());
        customer.setCname(getCname());
        customer.setCreatedDate(getCreatedDate());
        customer.setCristinId(getCristinId());
        customer.setDisplayName(getDisplayName());
        customer.setIdentifier(getIdentifier());
        customer.setInstitutionDns(getInstitutionDns());
        customer.setShortName(getShortName());
        customer.setFeideOrganizationId(getFeideOrganizationId());
        customer.setModifiedDate(getModifiedDate());
        customer.setVocabularies(getVocabularies());
        customer.setName(getName());
        customer.setLoginMethods(getLoginMethods());

        return customer;
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
    public Set<VocabularyDao> getVocabularies() {
        return nonNull(vocabularies) ? vocabularies : Collections.emptySet();
    }

    @Override
    public void setVocabularies(Set<VocabularyDao> vocabularies) {
        this.vocabularies = nonNull(vocabularies) ? vocabularies : Collections.emptySet();
    }

    @Override
    public LoginMethods getLoginMethods() {
        return loginMethods;
    }

    @Override
    public void setLoginMethods(LoginMethods loginMethods) {
        this.loginMethods = loginMethods;
    }

    public CustomerDto toCustomerDto() {
        CustomerDto customer = new CustomerDto();

        customer.setCname(getCname());
        customer.setName(getName());
        customer.setIdentifier(getIdentifier());
        customer.setArchiveName(getArchiveName());
        customer.setCreatedDate(getCreatedDate());
        customer.setDisplayName(getDisplayName());
        customer.setInstitutionDns(getInstitutionDns());
        customer.setShortName(getShortName());
        customer.setVocabularies(extractVocabularySettings());
        customer.setModifiedDate(getModifiedDate());
        customer.setFeideOrganizationId(getFeideOrganizationId());
        customer.setCristinId(getCristinId());
        customer.setLoginMethods(getLoginMethods());

        customer.setId(LinkedDataContextUtils.toId(getIdentifier()));

        return customer;
    }

    public static Set<VocabularyDao> extractVocabularySettings(WithVocabulary<VocabularyDto> dto) {
        return Optional.ofNullable(dto.getVocabularies())
            .stream()
            .flatMap(Collection::stream)
            .map(VocabularyDao::fromVocabularySettingsDto)
            .collect(Collectors.toSet());
    }

    private Set<VocabularyDto> extractVocabularySettings() {
        return Optional.ofNullable(this.getVocabularies())
            .stream()
            .flatMap(Collection::stream)
            .map(VocabularyDao::toVocabularySettingsDto)
            .collect(Collectors.toSet());
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
        CustomerDao that = (CustomerDao) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
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
                && Objects.equals(getLoginMethods(), that.getLoginMethods());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getCreatedDate(), getModifiedDate(), getName(), getDisplayName(),
                getShortName(), getArchiveName(), getCname(), getInstitutionDns(), getFeideOrganizationId(),
                getCristinId(), getVocabularies(), getLoginMethods());
    }
}
