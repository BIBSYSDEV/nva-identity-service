package no.unit.nva.customer.create;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

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

    public static CreateCustomerRequest fromCustomerDto(CustomerDto customerDto) {
        var request = new CreateCustomerRequest();
        request.setName(customerDto.getName());
        request.setDisplayName(customerDto.getDisplayName());
        request.setArchiveName(customerDto.getArchiveName());
        request.setCname(customerDto.getCname());
        request.setInstitutionDns(customerDto.getInstitutionDns());
        request.setFeideOrganizationDomain(customerDto.getFeideOrganizationDomain());
        request.setVocabularies(customerDto.getVocabularies());
        request.setType(customerDto.getType());
        return request;
    }

    public static CreateCustomerRequest fromJson(String json) {
        return attempt(() -> JsonConfig.beanFrom(CreateCustomerRequest.class, json))
            .orElseThrow(fail -> new BadRequestException("Could not parse input:" + json));
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
            .build();
    }

    @JacocoGenerated
    public String getName() {
        return name;
    }

    @JacocoGenerated
    public void setName(String name) {
        this.name = name;
    }

    @JacocoGenerated
    public String getDisplayName() {
        return displayName;
    }

    @JacocoGenerated
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JacocoGenerated
    public String getShortName() {
        return shortName;
    }

    @JacocoGenerated
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    @JacocoGenerated
    public String getArchiveName() {
        return archiveName;
    }

    @JacocoGenerated
    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    @JacocoGenerated
    public String getCname() {
        return cname;
    }

    @JacocoGenerated
    public void setCname(String cname) {
        this.cname = cname;
    }

    @JacocoGenerated
    public String getInstitutionDns() {
        return institutionDns;
    }

    @JacocoGenerated
    public void setInstitutionDns(String institutionDns) {
        this.institutionDns = institutionDns;
    }

    @JacocoGenerated
    public String getFeideOrganizationDomain() {
        return feideOrganizationDomain;
    }

    @JacocoGenerated
    public void setFeideOrganizationDomain(String feideOrganizationDomain) {
        this.feideOrganizationDomain = feideOrganizationDomain;
    }

    @JacocoGenerated
    public URI getCristinId() {
        return cristinId;
    }

    @JacocoGenerated
    public void setCristinId(URI cristinId) {
        this.cristinId = cristinId;
    }

    @JacocoGenerated
    public List<VocabularyDto> getVocabularies() {
        return vocabularies;
    }

    @JacocoGenerated
    public void setVocabularies(List<VocabularyDto> vocabularies) {
        this.vocabularies = vocabularies;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDisplayName(), getShortName(), getArchiveName(), getCname(),
                            getInstitutionDns(),
                            getFeideOrganizationDomain(), getCristinId(), getVocabularies());
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
               && Objects.equals(getVocabularies(), that.getVocabularies());
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.asString(this)).orElseThrow();
    }

    @JacocoGenerated
    public String getType() {
        return TYPE_VALUE;
    }

    public void setType(String type) {
         validateType(type);
    }

    private String validateType(String type) {
        if (!TYPE_VALUE.equals(type)) {
            throw new BadRequestException("Input request is not of type 'Customer'");
        }
        return TYPE_VALUE;
    }
}
