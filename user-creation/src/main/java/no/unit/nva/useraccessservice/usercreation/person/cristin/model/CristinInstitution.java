package no.unit.nva.useraccessservice.usercreation.person.cristin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CristinInstitution {
    @JsonProperty("corresponding_unit")
    private final CristinInstitutionUnit correspondingUnit;

    @JsonCreator
    public CristinInstitution(@JsonProperty("corresponding_unit") CristinInstitutionUnit correspondingUnit) {
        this.correspondingUnit = correspondingUnit;
    }

    public CristinInstitutionUnit getCorrespondingUnit() {
        return correspondingUnit;
    }
}
