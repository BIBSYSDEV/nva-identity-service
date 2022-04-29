package no.unit.nva.useraccessservice.usercreation;

import java.net.URI;
import java.util.Objects;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinAffiliation;

public class PersonEmployment {

    private URI child;
    private URI parent;
    private boolean active;

    protected PersonEmployment() {

    }

    public PersonEmployment(URI child, URI parent, boolean active) {
        this.child = child;
        this.parent = parent;
        this.active = active;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public URI getChild() {
        return child;
    }

    public void setChild(URI child) {
        this.child = child;
    }

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public CristinAffiliation toCristinAffiliation() {
        return CristinAffiliation.builder()
            .withOrganization(getChild())
            .withActive(isActive())
            .build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getChild(), getParent(), isActive());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersonEmployment)) {
            return false;
        }
        PersonEmployment that = (PersonEmployment) o;
        return isActive() == that.isActive()
               && Objects.equals(getChild(), that.getChild())
               && Objects.equals(getParent(), that.getParent());
    }

    public static class Builder {

        private PersonEmployment personEmployment;

        private Builder() {
            this.personEmployment = new PersonEmployment();
        }

        public Builder withChild(URI child) {
            personEmployment.setChild(child);
            return this;
        }

        public Builder withParent(URI parent) {
            personEmployment.setParent(parent);
            return this;
        }

        public Builder withActive(boolean active) {
            personEmployment.setActive(active);
            return this;
        }

        public PersonEmployment build() {
            return personEmployment;
        }
    }
}
