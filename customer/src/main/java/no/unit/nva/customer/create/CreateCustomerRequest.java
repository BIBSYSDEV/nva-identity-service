package no.unit.nva.customer.create;

import static java.util.Objects.nonNull;
import static no.unit.nva.customer.create.CreateCustomerRequest.TYPE_VALUE;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.model.PublicationWorkflow;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(TYPE_VALUE)
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

    public List<VocabularyDto> getVocabularies() {
        return vocabularies;
    }

    public void setVocabularies(List<VocabularyDto> vocabularies) {
        this.vocabularies = vocabularies;
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

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDisplayName(), getShortName(), getArchiveName(), getCname(),
                            getInstitutionDns(), getFeideOrganizationDomain(), getCristinId(), getVocabularies(),
                            getDoiAgent());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreateCustomerRequest)) {
            return false;
        }
        CreateCustomerRequest that = (CreateCustomerRequest) o;
        return Objects.equals(getName(), that.getName())
               && Objects.equals(getDisplayName(), that.getDisplayName())
               && Objects.equals(getShortName(), that.getShortName())
               && Objects.equals(getArchiveName(), that.getArchiveName())
               && Objects.equals(getCname(), that.getCname())
               && Objects.equals(getInstitutionDns(), that.getInstitutionDns())
               && Objects.equals(getFeideOrganizationDomain(), that.getFeideOrganizationDomain())
               && Objects.equals(getCristinId(), that.getCristinId())
               && Objects.equals(getDoiAgent(), that.getDoiAgent())
               && Objects.equals(getVocabularies(), that.getVocabularies());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
