package no.unit.nva.useraccessservice.usercreation.person.cristin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CristinPerson {

    @JsonProperty("cristin_person_id")
    private final String id;
    @JsonProperty("first_name")
    private final String firstname;
    @JsonProperty("surname")
    private final String surname;
    @JsonProperty("affiliations")
    private final List<CristinAffiliation> affiliations;

    @JsonCreator
    public CristinPerson(@JsonProperty("cristin_person_id") String id,
                         @JsonProperty("first_name") String firstname,
                         @JsonProperty("surname") String surname,
                         @JsonProperty("affiliations") List<CristinAffiliation> affiliations) {
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

    public List<CristinAffiliation> getAffiliations() {
        return affiliations;
    }
}
