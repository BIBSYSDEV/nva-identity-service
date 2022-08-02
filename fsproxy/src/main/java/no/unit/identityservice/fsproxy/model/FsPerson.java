package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsPerson {

    @JsonProperty("personlopenummer")
    private final FsIdNumber fsIdNumber;

    @JsonCreator
    public FsPerson(@JsonProperty("personlopenummer") FsIdNumber fsIdNumber) {
        this.fsIdNumber = fsIdNumber;
    }

    public FsIdNumber getFsIdNumber() {
        return fsIdNumber;
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
