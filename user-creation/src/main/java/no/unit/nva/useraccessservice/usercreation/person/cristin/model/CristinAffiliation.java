package no.unit.nva.useraccessservice.usercreation.person.cristin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CristinAffiliation {

    @JsonProperty("institution")
    private final CristinAffiliationInstitution institution;
    @JsonProperty("unit")
    private final CristinAffiliationUnit unit;
    @JsonProperty("active")
    private final boolean active;

    @JsonCreator
    public CristinAffiliation(@JsonProperty("institution") CristinAffiliationInstitution institution,
                              @JsonProperty("unit") CristinAffiliationUnit unit,
                              @JsonProperty("active") boolean active) {
        this.institution = institution;
        this.unit = unit;
        this.active = active;
    }

    public CristinAffiliationInstitution getInstitution() {
        return institution;
    }

    public CristinAffiliationUnit getUnit() {
        return unit;
    }

    public boolean isActive() {
        return active;
    }
}
