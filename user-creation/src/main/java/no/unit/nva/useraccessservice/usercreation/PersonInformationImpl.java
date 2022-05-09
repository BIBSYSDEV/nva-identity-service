package no.unit.nva.useraccessservice.usercreation;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.usercreation.cristin.PersonAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.core.JacocoGenerated;

public class PersonInformationImpl implements PersonInformation {

    private final String personFeideIdentifier;
    private final String orgFeideDomain;
    private CristinPersonResponse cristinResponse;
    private Set<CustomerDto> activeCustomers;

    private List<PersonAffiliation> personAffiliations;

    public PersonInformationImpl(String personFeideIdentifier, String orgFeideDomain) {
        this.personFeideIdentifier = personFeideIdentifier;
        this.orgFeideDomain = orgFeideDomain;
    }

    @JacocoGenerated
    @Override
    public Set<CustomerDto> getActiveCustomers() {
        return activeCustomers;
    }

    @JacocoGenerated
    @Override
    public void setActiveCustomers(Set<CustomerDto> activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    @JacocoGenerated
    @Override
    public String getOrgFeideDomain() {
        return orgFeideDomain;
    }

    @JacocoGenerated
    @Override
    public String getPersonFeideIdentifier() {
        return personFeideIdentifier;
    }

    @JacocoGenerated
    @Override
    public Optional<CristinPersonResponse> getCristinPersonResponse() {
        return Optional.ofNullable(this.cristinResponse);
    }

    @JacocoGenerated
    @Override
    public void setCristinPersonResponse(CristinPersonResponse cristinResponse) {
        this.cristinResponse = cristinResponse;
    }

    @Override
    public URI getOrganizationAffiliation(URI parentInstitution) {
        var affiliations = allAffiliationsWithSameParentInstitution(parentInstitution);
        return anyAffiliationButProduceConsistentResponseForSameInputSet(affiliations);
    }

    @JacocoGenerated
    @Override
    public List<PersonAffiliation> getPersonAffiliations() {
        return this.personAffiliations;
    }

    @Override
    @JacocoGenerated
    public void setPersonAffiliations(List<PersonAffiliation> affiliationInformation) {
        this.personAffiliations = affiliationInformation;
    }

    private Stream<URI> allAffiliationsWithSameParentInstitution(URI parentInstitution) {
        return this.personAffiliations.stream()
            .filter(affiliation -> affiliation.getParentInstitution().equals(parentInstitution))
            .map(PersonAffiliation::getOrganization);
    }

    private URI anyAffiliationButProduceConsistentResponseForSameInputSet(Stream<URI> affiliations) {
        return affiliations.map(URI::toString).sorted().map(URI::create).findFirst().orElseThrow();
    }
}
