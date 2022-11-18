package no.unit.nva.useraccessservice.usercreation;

import java.net.URI;

public final class PersonAffiliation {
    private final URI institutionCristinId;
    private final URI unitCristinId;

    private PersonAffiliation(URI institutionCristinId, URI unitCristinId) {
        this.institutionCristinId = institutionCristinId;
        this.unitCristinId = unitCristinId;
    }

    public static PersonAffiliation create(URI cristinInstitutionUri, URI cristinUnitUri) {
        return new PersonAffiliation(cristinInstitutionUri, cristinUnitUri);
    }

    public URI getInstitutionCristinId() {
        return institutionCristinId;
    }

    public URI getUnitCristinId() {
        return unitCristinId;
    }
}
