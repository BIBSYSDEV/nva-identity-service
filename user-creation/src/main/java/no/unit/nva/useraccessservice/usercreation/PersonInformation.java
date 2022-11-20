package no.unit.nva.useraccessservice.usercreation;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface PersonInformation {

    boolean personIsPresentInPersonRegistry();

    List<PersonAffiliation> getPersonAffiliations();

    Optional<URI> getPersonRegistryId();

    Optional<String> getGivenName();

    Optional<String> getFamilyName();

    URI getOrganizationAffiliation(URI parentInstitution);

    String getFeideIdentifier();

    String getFeideDomain();
}
