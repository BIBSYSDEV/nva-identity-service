package no.unit.nva.cognito;

import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class UserAffiliation {

    private URI organization;
    private URI parentInstitution;

    public static UserAffiliation create(URI organization, URI parentInstitution) {
        var pair = new UserAffiliation();
        pair.setOrganization(organization);
        pair.setParentInstitution(parentInstitution);
        return pair;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getOrganization(), getParentInstitution());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserAffiliation)) {
            return false;
        }
        UserAffiliation that = (UserAffiliation) o;
        return Objects.equals(getOrganization(), that.getOrganization()) && Objects.equals(
            getParentInstitution(), that.getParentInstitution());
    }

    @JacocoGenerated
    public URI getOrganization() {
        return organization;
    }

    @JacocoGenerated
    public void setOrganization(URI organization) {
        this.organization = organization;
    }

    @JacocoGenerated
    public URI getParentInstitution() {
        return parentInstitution;
    }

    @JacocoGenerated
    public void setParentInstitution(URI parentInstitution) {
        this.parentInstitution = parentInstitution;
    }
}
