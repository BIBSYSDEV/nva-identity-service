package no.unit.identityservice.fsproxy.model.staffperson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

public class FsRoleToStaffPerson {

    @JsonProperty("href")
    private final URI uriToRole;

    @JsonCreator
    public FsRoleToStaffPerson(@JsonProperty("href") URI uriToRole) {
        this.uriToRole = uriToRole;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(uriToRole);
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
        return Objects.equals(uriToRole, that.uriToRole);
    }

    @JsonIgnore
    public URI getUriToRole() {
        return uriToRole;
    }
}



