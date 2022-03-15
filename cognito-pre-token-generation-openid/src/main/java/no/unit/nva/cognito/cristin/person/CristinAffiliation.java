package no.unit.nva.cognito.cristin.person;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CristinAffiliation {

    private URI organization;
    private boolean active;

    public static Builder builder() {
        return new Builder();
    }

    @JacocoGenerated
    public String getOrganization() {
        return organization.toString();
    }

    @JacocoGenerated
    public void setOrganization(String organization) {
        this.organization = attempt(() -> URI.create(organization)).orElseThrow();
    }

    @JsonIgnore
    public URI getOrganizationUri() {
        return organization;
    }

    @JacocoGenerated
    public boolean isActive() {
        return active;
    }

    @JacocoGenerated
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString(){
        return attempt(()-> JsonConfig.objectMapper.asString(this)).orElseThrow();
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
}
