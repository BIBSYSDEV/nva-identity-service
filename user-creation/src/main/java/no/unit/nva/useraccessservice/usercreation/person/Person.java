package no.unit.nva.useraccessservice.usercreation.person;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

public class Person {

    private final URI id;
    private final String identifier;
    private final String firstname;
    private final String surname;
    private final List<Affiliation> affiliations;

    public Person(URI id, String identifier, String firstname, String surname, List<Affiliation> affiliations) {
        this.id = id;
        this.identifier = identifier;
        this.firstname = firstname;
        this.surname = surname;
        this.affiliations = affiliations;
    }

    public URI getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getSurname() {
        return surname;
    }

    public List<Affiliation> getAffiliations() {
        return affiliations;
    }

    public URI getConsistentUnitAffiliation(URI institutionId) {
        var affiliations = findInstitutionUnitIds(institutionId);
        return consistentlyPickAffiliationBySortingUriString(affiliations);
    }

    private URI consistentlyPickAffiliationBySortingUriString(Stream<URI> unitIds) {
        return unitIds.map(URI::toString).sorted().map(URI::create).findFirst().orElseThrow();
    }

    private Stream<URI> findInstitutionUnitIds(URI institutionId) {
        return this.affiliations.stream()
                   .filter(affiliation -> affiliation.getInstitutionId().equals(institutionId))
                   .map(Affiliation::getUnitIds)
                   .collect(SingletonCollector.collect()).stream();
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return "Person{"
               + "id=" + id
               + ", identifier='" + identifier + '\''
               + ", firstname='" + firstname + '\''
               + ", surname='" + surname + '\''
               + ", affiliations=" + affiliations
               + '}';
    }
}
