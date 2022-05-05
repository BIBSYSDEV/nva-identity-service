package no.unit.nva.useraccessservice.usercreation.cristin;

import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class PersonAffiliation {

    private URI organization;
    private URI parentInstitution;

    public static PersonAffiliation create(URI organization, URI parentInstitution) {
        var userAffiliation = new PersonAffiliation();
        userAffiliation.setOrganization(organization);
        userAffiliation.setParentInstitution(parentInstitution);
        return userAffiliation;
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
        if (!(o instanceof PersonAffiliation)) {
            return false;
        }
        PersonAffiliation that = (PersonAffiliation) o;
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
