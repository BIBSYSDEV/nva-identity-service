package no.unit.identityservice.fsproxy.model.fagperson;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsStaffPerson {

    private final FsRoleToStaffPerson roles;

    public FsStaffPerson(FsRoleToStaffPerson roles) {
        this.roles = roles;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(roles);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FsStaffPerson that = (FsStaffPerson) o;
        return Objects.equals(roles, that.roles);
    }

    public FsRoleToStaffPerson getRoles() {
        return roles;
    }
}
