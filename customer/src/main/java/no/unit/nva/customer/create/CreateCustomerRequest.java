package no.unit.nva.customer.create;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.model.CustomerDto.ServiceCenter;
import no.unit.nva.customer.model.PublicationInstanceTypes;
import no.unit.nva.customer.model.PublicationWorkflow;
import no.unit.nva.customer.model.Sector;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.nonNull;
import static no.unit.nva.customer.create.CreateCustomerRequest.TYPE_VALUE;
import static nva.commons.core.attempt.Try.attempt;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(TYPE_VALUE)
@SuppressWarnings({"PMD.TooManyFields", "PMD.GodClass", "PMD.ExcessivePublicCount"})
public class CreateCustomerRequest {

    public static final String TYPE_VALUE = "Customer";
    private String name;
    private String displayName;
    private String shortName;
    private String archiveName;
    private String cname;
    private String institutionDns;
    private String feideOrganizationDomain;
    private URI cristinId;
    private List<VocabularyDto> vocabularies;
    private PublicationWorkflow publicationWorkflow;
    private ApplicationDomain customerOf;
    private DoiAgentDto doiAgent;
    private boolean nviInstitution;
    private boolean rboInstitution;
    private boolean generalSupportEnabled;
    private Instant inactiveFrom;
    private Sector sector;
    private URI rorId;
    private ServiceCenter serviceCenter;
    private Set<PublicationInstanceTypes> allowFileUploadForTypes;

    public static CreateCustomerRequest fromCustomerDto(CustomerDto customerDto) {
        var request = new CreateCustomerRequest();
        request.setName(customerDto.getName());
        request.setDisplayName(customerDto.getDisplayName());
        request.setShortName(customerDto.getShortName());
        request.setArchiveName(customerDto.getArchiveName());
        request.setCname(customerDto.getCname());
        request.setInstitutionDns(customerDto.getInstitutionDns());
        request.setFeideOrganizationDomain(customerDto.getFeideOrganizationDomain());
        request.setVocabularies(customerDto.getVocabularies());
        request.setCristinId(customerDto.getCristinId());
        request.setPublicationWorkflow(customerDto.getPublicationWorkflow());
        request.setCustomerOf(customerDto.getCustomerOf());
        request.setSector(customerDto.getSector());
        request.setNviInstitution(customerDto.isNviInstitution());
        request.setRboInstitution(customerDto.isRboInstitution());
        request.setInactiveFrom(customerDto.getInactiveFrom());
        request.setRorId(customerDto.getRorId());
        request.setServiceCenter(customerDto.getServiceCenter());
        request.setAllowFileUploadForTypes(customerDto.getAllowFileUploadForTypes());
        request.setGeneralSupportEnabled(customerDto.isGeneralSupportEnabled());
        if (nonNull(customerDto.getDoiAgent())) {
            request.setDoiAgent(new DoiAgentDto(customerDto.getDoiAgent()));
        }
        return request;
    }

    public CustomerDto toCustomerDto() {
        return CustomerDto.builder()
            .withName(getName())
            .withDisplayName(getDisplayName())
            .withShortName(getShortName())
            .withArchiveName(getArchiveName())
            .withCname(getCname())
            .withInstitutionDns(getInstitutionDns())
            .withFeideOrganizationDomain(getFeideOrganizationDomain())
            .withCristinId(getCristinId())
            .withVocabularies(vocabularies)
            .withPublicationWorkflow(getPublicationWorkflow())
            .withCustomerOf(getCustomerOf())
            .withDoiAgent(getDoiAgent())
            .withNviInstitution(isNviInstitution())
            .withRboInstitution(isRboInstitution())
            .withInactiveFrom(getInactiveFrom())
            .withSector(getSector())
            .withRorId(getRorId())
            .withAllowFileUploadForTypes(getAllowFileUploadForTypes())
            .withServiceCenter(getServiceCenter())
            .withGeneralSupportEnabled(isGeneralSupportEnabled())
            .build();
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

    public PublicationWorkflow getPublicationWorkflow() {
        return nonNull(publicationWorkflow) ? publicationWorkflow
            : PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
    }

    public void setPublicationWorkflow(PublicationWorkflow publicationWorkflow) {
        this.publicationWorkflow = publicationWorkflow;
    }

    public ApplicationDomain getCustomerOf() {
        return customerOf;
    }

    public void setCustomerOf(ApplicationDomain customerOf) {
        this.customerOf = customerOf;
    }

    public DoiAgentDto getDoiAgent() {
        return doiAgent;
    }

    public void setDoiAgent(DoiAgentDto doiAgent) {
        this.doiAgent = doiAgent;
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

    public boolean isGeneralSupportEnabled() {
        return generalSupportEnabled;
    }

    public void setGeneralSupportEnabled(boolean generalSupportEnabled) {
        this.generalSupportEnabled = generalSupportEnabled;
    }

    public Instant getInactiveFrom() {
        return inactiveFrom;
    }

    public void setInactiveFrom(Instant inactiveFrom) {
        this.inactiveFrom = inactiveFrom;
    }

    public Sector getSector() {
        return nonNull(sector) ? sector : Sector.UHI;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public Set<PublicationInstanceTypes> getAllowFileUploadForTypes() {
        return allowFileUploadForTypes;
    }

    public void setAllowFileUploadForTypes(Set<PublicationInstanceTypes> allowFileUploadForTypes) {
        this.allowFileUploadForTypes = allowFileUploadForTypes;
    }

    public URI getRorId() {
        return rorId;
    }

    public void setRorId(URI rorId) {
        this.rorId = rorId;
    }

    public ServiceCenter getServiceCenter() {
        return serviceCenter;
    }

    public void setServiceCenter(ServiceCenter serviceCenterUri) {
        this.serviceCenter = serviceCenterUri;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDisplayName(), getShortName(), getArchiveName(), getCname(),
            getInstitutionDns(), getFeideOrganizationDomain(), getCristinId(), getVocabularies(),
            getDoiAgent(), getSector(), isNviInstitution(), isRboInstitution(), getInactiveFrom(),
            getRorId(), getServiceCenter(), getAllowFileUploadForTypes());
    }

    public List<VocabularyDto> getVocabularies() {
        return vocabularies;
    }

    public void setVocabularies(List<VocabularyDto> vocabularies) {
        this.vocabularies = vocabularies;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreateCustomerRequest that)) {
            return false;
        }
        return Objects.equals(getName(), that.getName())
            && Objects.equals(getDisplayName(), that.getDisplayName())
            && Objects.equals(getShortName(), that.getShortName())
            && Objects.equals(getArchiveName(), that.getArchiveName())
            && Objects.equals(getCname(), that.getCname())
            && Objects.equals(getInstitutionDns(), that.getInstitutionDns())
            && Objects.equals(getFeideOrganizationDomain(), that.getFeideOrganizationDomain())
            && Objects.equals(getCristinId(), that.getCristinId())
            && Objects.equals(getDoiAgent(), that.getDoiAgent())
            && Objects.equals(getSector(), that.getSector())
            && Objects.equals(getRorId(), that.getRorId())
            && Objects.equals(getServiceCenter(), that.getServiceCenter())
            && Objects.equals(isNviInstitution(), that.isNviInstitution())
            && Objects.equals(isRboInstitution(), that.isRboInstitution())
            && Objects.equals(getInactiveFrom(), that.getInactiveFrom())
            && Objects.equals(getVocabularies(), that.getVocabularies())
            && Objects.equals(getAllowFileUploadForTypes(), that.getAllowFileUploadForTypes())
            && Objects.equals(isGeneralSupportEnabled(), that.isGeneralSupportEnabled());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
