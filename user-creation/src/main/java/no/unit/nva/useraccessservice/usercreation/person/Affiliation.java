package no.unit.nva.useraccessservice.usercreation.person;

import java.net.URI;
import java.util.List;

public class Affiliation {

    private final URI institutionUri;
    private final List<URI> unitUris;

    public Affiliation(URI institutionUri, List<URI> unitUris) {
        this.institutionUri = institutionUri;
        this.unitUris = unitUris;
    }

    public URI getInstitutionUri() {
        return institutionUri;
    }

    public List<URI> getUnitUris() {
        return unitUris;
    }

    @Override
    public String toString() {
        return "Affiliation{"
               + "institutionUri='" + institutionUri + '\''
               + ", unitUris=" + unitUris
               + '}';
    }
}
