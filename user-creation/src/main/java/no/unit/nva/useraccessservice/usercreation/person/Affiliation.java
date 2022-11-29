package no.unit.nva.useraccessservice.usercreation.person;

import java.net.URI;
import java.util.List;
import nva.commons.core.JacocoGenerated;

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
