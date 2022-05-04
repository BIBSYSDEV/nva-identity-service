package no.unit.nva.useraccessservice.usercreation;

import java.net.URI;
import java.util.List;
import java.util.Set;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.usercreation.cristin.PersonAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;

public interface PersonInformation {

    Set<CustomerDto> getActiveCustomers();

    void setActiveCustomers(Set<CustomerDto> activeCustomers);

    String getOrgFeideDomain();

    String getPersonFeideIdentifier();

    CristinPersonResponse getCristinPersonResponse();

    void setCristinPersonResponse(CristinPersonResponse cristinResponse);

    URI getOrganizationAffiliation(URI parentInstitution);

    List<PersonAffiliation> getPersonAffiliations();

    void setPersonAffiliations(List<PersonAffiliation> affiliationInformation);
}