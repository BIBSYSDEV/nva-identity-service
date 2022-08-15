package no.unit.identityservice.fsproxy.model.staffperson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsRoleToStaffPerson {

    @JsonProperty("href")
    private final String hrefToRole;

    @JsonCreator
    public FsRoleToStaffPerson(@JsonProperty("href") String hrefToRole) {
        this.hrefToRole = hrefToRole;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(hrefToRole);
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
        FsRoleToStaffPerson that = (FsRoleToStaffPerson) o;
        return Objects.equals(hrefToRole, that.hrefToRole);
    }

    @JsonIgnore
    public String getUriToRole() {
        return hrefToRole;
    }
}



