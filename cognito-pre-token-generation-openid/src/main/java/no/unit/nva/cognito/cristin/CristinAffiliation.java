package no.unit.nva.cognito.cristin;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CristinAffiliation {


    private URI organization;
    private boolean active;
    private CristinRole role;

    @JacocoGenerated
    public String getOrganization() {
        return organization.toString();
    }

    @JsonIgnore
    public URI getOrganizationUri(){
        return organization;
    }

    @JacocoGenerated
    public void setOrganization(String organization) {
        this.organization = attempt(()->URI.create(organization)).orElseThrow();
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
