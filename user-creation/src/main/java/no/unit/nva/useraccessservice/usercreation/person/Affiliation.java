package no.unit.nva.useraccessservice.usercreation.person;

import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.List;

public class Affiliation {

    private final URI institutionId;
    private final List<URI> unitIds;

    public Affiliation(URI institutionId, List<URI> unitIds) {
        this.institutionId = institutionId;
        this.unitIds = unitIds;
    }

    public URI getInstitutionId() {
        return institutionId;
    }

    public List<URI> getUnitIds() {
        return unitIds;
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return "Affiliation{"
                + "institutionUri='" + institutionId + '\''
                + ", unitUris=" + unitIds
                + '}';
    }
}
