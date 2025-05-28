package no.unit.nva.useraccessservice.usercreation.person.cristin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CristinPerson {

    @JsonProperty("cristin_person_id")
    private final String id;
    @JsonProperty("first_name")
    private final String firstname;
    @JsonProperty("surname")
    private final String surname;
    @JsonProperty("affiliations")
    private final List<CristinAffiliation> affiliations;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("norwegian_national_id")
    private final String norwegianNationalId;

    @JsonCreator
    public CristinPerson(@JsonProperty("cristin_person_id") String id,
                         @JsonProperty("first_name") String firstname,
                         @JsonProperty("surname") String surname,
                         @JsonProperty("affiliations") List<CristinAffiliation> affiliations,
                         @JsonProperty("norwegian_national_id") String norwegianNationalId) {
        this.id = id;
        this.firstname = firstname;
        this.surname = surname;
        this.affiliations = Objects.isNull(affiliations) ? Collections.emptyList() : affiliations;
        this.norwegianNationalId = norwegianNationalId;
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

    public String getNorwegianNationalId() {
        return norwegianNationalId;
    }
}
