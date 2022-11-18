package no.unit.nva.useraccessservice.usercreation;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import nva.commons.core.JacocoGenerated;

public class PersonInformationImpl implements PersonInformation {
    private final String feideIdentifier;
    private final String feideDomain;
    private final Person personFromRegistry;

    private final List<PersonAffiliation> personAffiliations;

    public PersonInformationImpl(PersonRegistry personRegistry, String nin) {
        this(personRegistry, nin, null, null);
    }

    public PersonInformationImpl(PersonRegistry personRegistry, String nin, String feideIdentifier,
                                 String feideDomain) {
        this.feideIdentifier = feideIdentifier;
        this.feideDomain = feideDomain;
        Optional<Person> person = personRegistry.fetchPersonByNin(nin);
        if (person.isPresent()) {
            this.personFromRegistry = person.get();
            this.personAffiliations = new ArrayList<>();
            this.personFromRegistry.getAffiliations().stream()
                .map(this::createPersonAffiliations)
                .forEach(this.personAffiliations::addAll);
        } else {
            this.personFromRegistry = null;
            this.personAffiliations = Collections.emptyList();
        }
    }

    @Override
    public boolean personIsPresentInPersonRegistry() {
        return personFromRegistry != null;
    }

    @Override
    public Optional<String> getGivenName() {
        return (personFromRegistry != null) ? Optional.ofNullable(personFromRegistry.getFirstname()) : Optional.empty();
    }

    @Override
    public Optional<String> getFamilyName() {
        return (personFromRegistry != null) ? Optional.ofNullable(personFromRegistry.getSurname()) : Optional.empty();
    }

    @JacocoGenerated
    @Override
    public String getFeideDomain() {
        return this.feideDomain;
    }

    @JacocoGenerated
    @Override
    public String getFeideIdentifier() {
        return this.feideIdentifier;
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
    public Optional<URI> getPersonRegistryId() {
        return (personFromRegistry != null)
                   ? Optional.of(personFromRegistry.getId())
                   : Optional.empty();
    }

    private List<PersonAffiliation> createPersonAffiliations(Affiliation affiliation) {
        return affiliation.getUnitUris().stream()
                   .map(unitUri -> createPersonAffiliation(affiliation.getInstitutionUri(), unitUri))
                   .collect(Collectors.toList());
    }

    private PersonAffiliation createPersonAffiliation(URI institutionUri, URI unitUri) {
        return PersonAffiliation.create(institutionUri, unitUri);
    }

    private Stream<URI> allAffiliationsWithSameParentInstitution(URI parentInstitution) {
        return this.personAffiliations.stream()
                   .filter(affiliation -> affiliation.getInstitutionCristinId().equals(parentInstitution))
                   .map(PersonAffiliation::getUnitCristinId);
    }

    private URI anyAffiliationButProduceConsistentResponseForSameInputSet(Stream<URI> affiliations) {
        return affiliations.map(URI::toString).sorted().map(URI::create).findFirst().orElseThrow();
    }
}
