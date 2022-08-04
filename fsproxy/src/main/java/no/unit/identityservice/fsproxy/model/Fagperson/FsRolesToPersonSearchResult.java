package no.unit.identityservice.fsproxy.model.Fagperson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsRolesToPersonSearchResult {

    @JsonProperty("items")
    private List<FsRoleToStaffPerson> items;

    @JsonCreator
    public FsRolesToPersonSearchResult(@JsonProperty("items") List<FsRoleToStaffPerson> items) {
        this.items = items;
    }

    public List<FsRoleToStaffPerson> getItems() {
        return items;
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
        FsRolesToPersonSearchResult that = (FsRolesToPersonSearchResult) o;
        return Objects.equals(items, that.items);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(items);
    }
}
