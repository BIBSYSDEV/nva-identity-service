package no.unit.identityservice.fsproxy.model.Fagperson;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FsPossibleStaffPerson {

    @JsonProperty("aktiv")
    private boolean isActive;

    public FsPossibleStaffPerson(@JsonProperty("aktiv") Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getActiveStatus() {
        return isActive;
    }
}
