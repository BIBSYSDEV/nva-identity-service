package no.unit.nva.cognito.cristin;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CristinAffiliation {

    private URI organization;
    private boolean active;
    private CristinRole role;

    public static Builder builder() {
        return new Builder();
    }

    @JacocoGenerated
    public String getOrganization() {
        return organization.toString();
    }

    @JsonIgnore
    public URI getOrganizationUri() {
        return organization;
    }

    @JacocoGenerated
    public void setOrganization(String organization) {
        this.organization = attempt(() -> URI.create(organization)).orElseThrow();
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

    public static class Builder {

        private final CristinAffiliation affiliation;

        private Builder() {
            affiliation = new CristinAffiliation();
        }

        public Builder withOrganization(URI organization) {
            affiliation.setOrganization(organization.toString());
            return this;
        }

        public Builder withActive(boolean active) {
            affiliation.setActive(active);
            return this;
        }

        public Builder withRole(CristinRole role) {
            affiliation.setRole(role);
            return this;
        }

        public CristinAffiliation build() {
            return this.affiliation;
        }
    }

    public String toString(){
        return attempt(()-> JsonConfig.objectMapper.asString(this)).orElseThrow();
    }
}
