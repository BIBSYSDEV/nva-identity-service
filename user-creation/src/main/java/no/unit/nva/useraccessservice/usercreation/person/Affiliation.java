package no.unit.nva.useraccessservice.usercreation.person;

import java.util.List;

public class Affiliation {
    private final String institutionId;
    private final List<String> unitId;

    public Affiliation(String institutionId, List<String> unitId) {
        this.institutionId = institutionId;
        this.unitId = unitId;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public List<String> getUnitId() {
        return unitId;
    }

    @Override
    public String toString() {
        return "Affiliation{" +
               "institutionId='" + institutionId + '\'' +
               ", unitId=" + unitId +
               '}';
    }
}
