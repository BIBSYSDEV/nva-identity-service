package no.unit.identityservice.fsproxy.model.Person;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.identityservice.fsproxy.model.Fagperson.FsPossibleStaffPerson;
import nva.commons.core.JacocoGenerated;

public class FsPerson {

    @JsonProperty("personlopenummer")
    private final FsIdNumber fsIdNumber;

    @JsonProperty("fagperson")
    private FsPossibleStaffPerson fagperson;

    @JsonCreator
    public FsPerson(@JsonProperty("personlopenummer") FsIdNumber fsIdNumber,
                    @JsonProperty("fagperson") FsPossibleStaffPerson fagperson) {
        this.fsIdNumber = fsIdNumber;
        this.fagperson = fagperson;
    }

    public FsIdNumber getFsIdNumber() {
        return fsIdNumber;
    }

    public FsPossibleStaffPerson getFagperson() {
        return fagperson;
    }

    @SuppressWarnings("PMD")
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FsPerson that = (FsPerson) o;
        return Objects.equals(this.fsIdNumber, that.getFsIdNumber());
    }


}
