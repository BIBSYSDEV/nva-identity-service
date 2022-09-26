package no.unit.nva.useraccessservice.usercreation.cristin.person;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.Objects;
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
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getOrganization(), isActive());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CristinAffiliation)) {
            return false;
        }
        CristinAffiliation that = (CristinAffiliation) o;
        return isActive() == that.isActive() && Objects.equals(getOrganization(), that.getOrganization());
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
        
        public CristinAffiliation build() {
            return this.affiliation;
        }
    }
}
