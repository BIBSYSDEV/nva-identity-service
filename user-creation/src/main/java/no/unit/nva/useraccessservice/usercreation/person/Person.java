package no.unit.nva.useraccessservice.usercreation.person;

import java.util.List;

public class Person {

    private final String id;
    private final String firstname;
    private final String surname;
    private final List<Affiliation> affiliations;

    public Person(String id, String firstname, String surname, List<Affiliation> affiliations) {
        this.id = id;
        this.firstname = firstname;
        this.surname = surname;
        this.affiliations = affiliations;
    }

    public String getId() {
        return id;
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
               "id='" + id + '\'' +
               ", firstname='" + firstname + '\'' +
               ", surname='" + surname + '\'' +
               ", affiliations=" + affiliations +
               '}';
    }
}
