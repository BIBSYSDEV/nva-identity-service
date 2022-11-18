package no.unit.nva.useraccessservice.usercreation.person;

import java.net.URI;
import java.util.List;

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

    @Override
    public String toString() {
        return "Person{" +
               "id=" + id +
               ", identifier='" + identifier + '\'' +
               ", firstname='" + firstname + '\'' +
               ", surname='" + surname + '\'' +
               ", affiliations=" + affiliations +
               '}';
    }
}
