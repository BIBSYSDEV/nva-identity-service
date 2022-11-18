package no.unit.nva.useraccessservice.usercreation;

import java.net.URI;

public final class PersonAffiliation {
    private final URI institutionCristinId;
    private final URI unitCristinId;

    private PersonAffiliation(URI institutionCristinId, URI unitCristinId) {
        this.institutionCristinId = institutionCristinId;
        this.unitCristinId = unitCristinId;
    }

    public static PersonAffiliation create(URI institutionCristinId, URI unitCristinId) {
        return new PersonAffiliation(institutionCristinId, unitCristinId);
    }

    public URI getInstitutionCristinId() {
        return institutionCristinId;
    }

    public URI getUnitCristinId() {
        return unitCristinId;
    }

    @Override
    public String toString() {
        return "PersonAffiliation{"
               + "institutionCristinId=" + institutionCristinId
               + ", unitCristinId=" + unitCristinId
               + '}';
    }
}
