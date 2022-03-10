package no.unit.nva.cognito.cristin;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CristinAffiliation {

    private URI affiliation;
    private boolean active;
    private CristinRole role;

    @JacocoGenerated
    public String getAffiliation() {
        return affiliation.toString();
    }

    @JacocoGenerated
    public void setAffiliation(String affiliation) {
        this.affiliation = attempt(()->URI.create(affiliation)).orElseThrow();
    }

    @JacocoGenerated
    public boolean isActive() {
        return active;
    }

    @JacocoGenerated
    public void setActive(boolean active) {
        this.active = active;
    }

    @JacocoGenerated
    public CristinRole getRole() {
        return role;
    }

    @JacocoGenerated
    public void setRole(CristinRole role) {
        this.role = role;
    }
}
